package me.vzhilin.mediaserver.util.scheduler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import me.vzhilin.mediaserver.InterleavedFrame;
import me.vzhilin.mediaserver.media.PullSource;
import me.vzhilin.mediaserver.media.impl.file.MediaPacket;
import me.vzhilin.mediaserver.server.RtpEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

final class PushTask implements Runnable {
    private final BufferingLimits limits;
    private final PullSource unbuffered;
    private final ScheduledExecutorService executor;
    private final PushListener listener;

    private boolean started;
    private boolean finished;
    private long startTimeMillis;
    private long startDtsMillis;
    private final RtpEncoder interleavedEncoder = new RtpEncoder();
    private ScheduledFuture<?> advanceFuture = null;

    private long lastDts;

    PushTask(PullSource pullSource,
            BufferingLimits limits,
            ScheduledExecutorService executor,
            PushListener listener) {
        this.limits = limits;
        this.unbuffered = pullSource;
        this.executor = executor;
        this.listener = listener;
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

    @Override
    public void run() {
        List<MediaPacket> ps = new ArrayList<>();
        synchronized (this) {
            if (finished) {
                return;
            }
            long sz = 0;
            long np = 0;
            long deltaPositionMillis = 0;
            MediaPacket pkt = null;
            while (unbuffered.hasNext() &&
                   limits.check(sz, np, deltaPositionMillis)) {
                pkt = unbuffered.next();
                ensureStarted(pkt);
                ps.add(pkt);
                sz += pkt.size();
                np += 1;
                deltaPositionMillis = (startDtsMillis - pkt.getDts()) - (startTimeMillis - pkt.getPts());
            }
            if (!ps.isEmpty()) {
                lastDts = Math.max(0, pkt.getDts());
            }
        }
        if (!ps.isEmpty()) {
            listener.next(new PushedPacket(this::pushNext, encodeInterleavedFrames(ps)));
        } else {
            listener.end();
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
