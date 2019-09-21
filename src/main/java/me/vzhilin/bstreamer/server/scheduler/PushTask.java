package me.vzhilin.bstreamer.server.scheduler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import me.vzhilin.bstreamer.server.RtpEncoder;
import me.vzhilin.bstreamer.server.media.InterleavedFrame;
import me.vzhilin.bstreamer.server.streaming.base.PullSource;
import me.vzhilin.bstreamer.server.streaming.file.MediaPacket;
import me.vzhilin.bstreamer.server.streaming.file.SourceDescription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

final class PushTask implements Runnable {
    private final BufferingLimits limits;
    private final ScheduledExecutorService executor;
    private PullSource unbuffered;
    private final Supplier<PullSource> sourceSupplier;

    private boolean started;
    private boolean finished;
    private long startTimeMillis;
    private long startDtsMillis;
    private final RtpEncoder interleavedEncoder = new RtpEncoder();
    private ScheduledFuture<?> advanceFuture = null;

    private long lastDts;

    private List<PushTaskSubscriber> subs = new ArrayList<>();
    private SourceDescription desc;

    PushTask(Supplier<PullSource> pullSource,
             BufferingLimits limits,
             ScheduledExecutorService executor) {
        this.limits = limits;
        this.sourceSupplier = pullSource;
        this.executor = executor;
    }

    public SourceDescription describe() {
        synchronized (this) {
            if (desc == null) {
                PullSource pullSource = sourceSupplier.get();
                desc = pullSource.getDesc();
                try {
                    pullSource.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return desc;
        }
    }

    public PushTaskSession subscribe(PushTaskSubscriber sub) {
        synchronized (this) {
            boolean wasEmpty = subs.isEmpty();
            subs.add(sub);

            if (wasEmpty) {
                startTimeMillis = System.currentTimeMillis();
                unbuffered = sourceSupplier.get();
            }
        }

        return new PushTaskSession(() -> unsubscribe(sub));
    }

    private void unsubscribe(PushTaskSubscriber sub) {
        synchronized (this) {
            if (subs.remove(sub) && subs.isEmpty()) {
                try {
                    unbuffered.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void pushNext() {
        long nowMillis = System.currentTimeMillis();
        synchronized (this) {
            if (finished) {
                return;
            }
            long delay = (startTimeMillis - nowMillis) - (startDtsMillis - lastDts);
            // TODO handle the situation the when delay is negative
            advanceFuture = executor.schedule(this, delay, TimeUnit.MILLISECONDS);
        }
    }

    private final List<PushTaskSubscriber> localSubs = new ArrayList<>();

    @Override
    public void run() {
        List<MediaPacket> ps = new ArrayList<>();

        synchronized (this) {
            localSubs.clear();
            localSubs.addAll(subs);

            if (finished) {
                return;
            }
            long sz = 0;
            long np = 0;
            long deltaPositionMillis = 0;
            MediaPacket pkt = null;
            long now = System.currentTimeMillis();
            while (unbuffered.hasNext() &&
                   limits.check(sz, np, deltaPositionMillis)) {
                pkt = unbuffered.next();
                ensureStarted(pkt);
                ps.add(pkt);
                sz += pkt.size();
                np += 1;
                deltaPositionMillis = (Math.max(0L, pkt.getDts()) - startDtsMillis) - (now - startTimeMillis);
            }
            if (!ps.isEmpty()) {
                lastDts = Math.max(0, pkt.getDts());
            }
        }

        boolean endReached = ps.isEmpty();
        int subsCount = localSubs.size();
        InterleavedFrame packet = encodeInterleavedFrames(ps);
        packet.retain(subsCount - 1);
        PushedPacket pp = new PushedPacket(this::pushNext, packet, subsCount);
        for (PushTaskSubscriber sub : localSubs) {
            if (endReached) {
                sub.onEnd();
            } else {
                sub.onNext(pp);
            }
        }
    }

    private void ensureStarted(MediaPacket pkt) {
        if (!started) {
            started = true;
            startTimeMillis = System.currentTimeMillis();
            startDtsMillis = pkt.getDts();
            if (startDtsMillis == Long.MIN_VALUE) {
                startDtsMillis = 0;
            }
        }
    }

    private int estimateSize(List<MediaPacket> packets) {
        int sz = 0;
        for (int i = 0; i < packets.size(); i++) {
            int payloadSize = packets.get(i).size();
            sz += interleavedEncoder.estimateSize(payloadSize);
        }
        return sz;
    }

    private InterleavedFrame encodeInterleavedFrames(List<MediaPacket> packets) {
        int interleavedFrameSize = estimateSize(packets);
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(interleavedFrameSize, interleavedFrameSize);
        for (int i = 0; i < packets.size(); i++) {
            MediaPacket pkt = packets.get(i);
            interleavedEncoder.encode(buffer, pkt, pkt.getDts() * 90);
        }
        packets.forEach(mediaPacket -> mediaPacket.getPayload().release());
        return new InterleavedFrame(buffer);
    }

    public void finish() {
        synchronized (this) {
            this.finished = true;
            if (advanceFuture != null) {
                advanceFuture.cancel(false);
            }
        }
    }
}
