package me.vzhilin.mediaserver.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import me.vzhilin.mediaserver.InterleavedFrame;
import me.vzhilin.mediaserver.media.PullSource;
import me.vzhilin.mediaserver.media.impl.file.MediaPacket;
import me.vzhilin.mediaserver.server.RtpEncoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BufferedPacketSource {
    private final PullSource unbuffered;
    private final ScheduledExecutorService executor;
    private final BufferedMediaPacketListener listener;
    private final Task task;
    private final BufferingLimits limits;

    private boolean started;
    private boolean stopped;

    public BufferedPacketSource(PullSource unbuffered,
                                BufferedMediaPacketListener listener,
                                BufferingLimits limits) {
        this.limits = limits;
        this.unbuffered = unbuffered;
        // fixme bind scheduled executor to buffered source
        this.executor = Executors.newScheduledThreadPool(4);
        this.listener = listener;
        task = new Task();
    }

    public void start() {
        if (!started) {
            started = true;
            executor.execute(task);
        }
    }

    public void stop() throws IOException {
        if (!stopped) {
            stopped = true;
            synchronized (task) {
                task.close();
                executor.shutdown();
                unbuffered.close();
            }
        }
    }

    private final class Task implements Runnable {
        private boolean started;
        private boolean closed;
        private long startTimeMillis;
        private long startDtsMillis;
        private final RtpEncoder encoder = new RtpEncoder();
        private ScheduledFuture<?> advanceFuture = null;

        private void advance(long dtsMillis) {
            if (closed) {
                return;
            }
            long timeMillis = System.currentTimeMillis();
            if (dtsMillis == Long.MIN_VALUE) {
                dtsMillis = 0;
            }
            long delay = (startTimeMillis - timeMillis) - (startDtsMillis - dtsMillis);
            // TODO handle the situation when delay is negative
            advanceFuture = executor.schedule(this, delay, TimeUnit.MILLISECONDS);
        }

        @Override
        public void run() {
            List<MediaPacket> ps = new ArrayList<>();
            BufferedMediaPacket bmp = null;
            synchronized (Task.this) {
                if (closed) {
                    return;
                }
                long sz = 0;
                long np = 0;
                long deltaPositionMillis = 0;
                MediaPacket pkt = null;
                while (unbuffered.hasNext() && limits.check(sz, np, deltaPositionMillis)) {
                    pkt = unbuffered.next();
                    ps.add(pkt);
                    if (!started) {
                        started = true;
                        startTimeMillis = System.currentTimeMillis();
                        startDtsMillis = pkt.getDts();
                        if (startDtsMillis == Long.MIN_VALUE) {
                            startDtsMillis = 0;
                        }
                    }
                    sz += pkt.size();
                    np += 1;
                    deltaPositionMillis = (startDtsMillis - pkt.getDts()) - (startTimeMillis - pkt.getPts());
                }
                if (!ps.isEmpty()) {
                    bmp = new BufferedMediaPacket(encodeInterleavedFrames(ps), pkt.getDts());
                }
            }
            if (!ps.isEmpty()) {
                listener.next(bmp);
            } else {
                listener.end();
            }
        }

        private int estimateSize(List<MediaPacket> packets) {
            int sz = 0;
            for (int i = 0; i < packets.size(); i++) {
                sz += encoder.estimateSize(packets.get(i));
            }
            return sz;
        }

        private InterleavedFrame encodeInterleavedFrames(List<MediaPacket> packets) {
            final int npackets = packets.size();
            int sz = estimateSize(packets);
            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(sz, sz);
            for (int i = 0; i < npackets; i++) {
                MediaPacket pkt = packets.get(i);
                encoder.encode(buffer, pkt, pkt.getDts() * 90);
            }
            packets.forEach(mediaPacket -> mediaPacket.getPayload().release());
            return new InterleavedFrame(buffer);
        }

        public void close() {
            this.closed = true;
            if (advanceFuture != null) {
                advanceFuture.cancel(false);
            }
        }
    }

    public final class BufferedMediaPacket {
        private final InterleavedFrame packet;
        private final long dts;
        private boolean drained;

        private BufferedMediaPacket(InterleavedFrame packet, long dts) {
            this.packet = packet;
            this.dts = dts;
        }

        public InterleavedFrame drain() {
            if (!drained) {
                drained = true;
                task.advance(dts);
            }
            return packet;
        }
    }

    public interface BufferedMediaPacketListener {
        void next(BufferedMediaPacket bufferedMediaPacket);
        void end();
    }

    public final static class BufferingLimits {
        private final long bytes;
        private final int npackets;
        private final long timeMillis;

        public BufferingLimits(long bytes, int npackets, long timeMillis) {
            this.bytes = bytes;
            this.npackets = npackets;
            this.timeMillis = timeMillis;
        }

        public boolean check(long sz, long np, long deltaPositionMillis) {
            return sz < this.bytes &&
                   np < this.npackets &&
                   deltaPositionMillis < this.timeMillis;
        }
    }
}
