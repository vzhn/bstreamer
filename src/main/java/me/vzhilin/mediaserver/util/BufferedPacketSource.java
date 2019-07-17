package me.vzhilin.mediaserver.util;

import me.vzhilin.mediaserver.media.MediaPacketSource;
import me.vzhilin.mediaserver.media.file.MediaPacket;

import java.io.IOException;
import java.util.concurrent.*;

public class BufferedPacketSource {
    private final MediaPacketSource unbuffered;
    private final ScheduledExecutorService executor;
    private final BufferedMediaPacketListener listener;
    private final Task task;
    private boolean started;
    private boolean stopped;

    public BufferedPacketSource(MediaPacketSource unbuffered, BufferedMediaPacketListener listener) {
        this.unbuffered = unbuffered;
        // fixme bind scheduled executor to each buffered source
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
                unbuffered.close();
            }
        }
    }

    private final class Task implements Runnable {
        private boolean started;
        private boolean closed;
        private long startTimeMillis;
        private long startDtsMillis;

        private void advance(long dtsMillis) {
            long timeMillis = System.currentTimeMillis();
            long delay = (startTimeMillis - timeMillis) - (startDtsMillis - dtsMillis);
            // TODO handle the situation when delay is negative
            executor.schedule(this, delay, TimeUnit.MILLISECONDS);
        }

        @Override
        public void run() {
            MediaPacket pkt = null;
            synchronized (Task.this) {
                if (!closed && unbuffered.hasNext()) {
                    pkt = unbuffered.next();
                    if (!started) {
                        started = true;
                        startTimeMillis = System.currentTimeMillis();
                        startDtsMillis = pkt.getDts();
                    }
                }
            }
            if (pkt != null) {
                listener.dataReady(new BufferedMediaPacket(pkt));
            } else {
                listener.eof();
            }
        }

        public void close() {
            this.closed = true;
        }
    }

    public final class BufferedMediaPacket {
        private final MediaPacket packet;
        private boolean drained;

        private BufferedMediaPacket(MediaPacket packet) {
            this.packet = packet;
        }

        public MediaPacket drain() {
            if (!drained) {
                drained = true;
                task.advance(packet.getDts());
            }
            return packet;
        }
    }

    public interface BufferedMediaPacketListener {
        void dataReady(BufferedMediaPacket bufferedMediaPacket);
        void eof();
    }
}
