package me.vzhilin.mediaserver.media;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ForkJoinPool;

public class BufferedMediaPacketSource implements MediaPacketSource {
    private final MediaPacketSource unbufferedSource;
    private final ForkJoinPool pool;
    private final Queue<MediaPacket> packetQueue = new LinkedList<>();
    private final static TaskResult EOF = new TaskResult(null);
    private final int queueSize;
    private final ArrayBlockingQueue<TaskResult> taskResultQueue;
    private final List<TaskResult> drainedPackets = new ArrayList<>();
    private final Runnable populateTask;
    private boolean eof;
    private int populateCount;
    private boolean started;

    public BufferedMediaPacketSource(MediaPacketSource unbufferedSource, int bufferSize) {
        this.unbufferedSource = unbufferedSource;
        this.pool = ForkJoinPool.commonPool();
        taskResultQueue = new ArrayBlockingQueue<>(bufferSize);
        this.queueSize = bufferSize;
        this.populateTask = new PopulateTask(bufferSize);
    }
    @Override
    public synchronized MediaPacketSourceDescription getDesc() {
        return unbufferedSource.getDesc();
    }
    @Override
    public boolean hasNext() {
        ensureStarted();
        if (packetQueue.isEmpty()) {
            if (eof) {
                return true;
            } else {
                drainPackets();
                return packetQueue.isEmpty();
            }
        } else {
            return true;
        }
    }
    @Override
    public MediaPacket next() {
        ensureStarted();
        if (packetQueue.isEmpty()) {
            if (eof) {
                return null;
            } else {
                drainPackets();
            }
        }

        return packetQueue.poll();
    }
    private void drainPackets() {
        try {
            drainedPackets.add(taskResultQueue.take());
            taskResultQueue.drainTo(drainedPackets, queueSize - 1);
            populateCount += drainedPackets.size();
            for (TaskResult tr: drainedPackets) {
                if (tr == EOF) {
                    eof = true;
                } else {
                    packetQueue.offer(tr.next());
                }
            }
            drainedPackets.clear();
            if (populateCount > queueSize / 2) {
                populateCount = 0;
                pool.execute(populateTask);
            }
        } catch (InterruptedException ex) {
            eof = true;
            // nop
        }
    }
    private void ensureStarted() {
        if (!started) {
            started = true;
            populateCount = 0;
            pool.execute(populateTask);
        }
    }
    @Override
    public void close() throws IOException {
        synchronized (unbufferedSource) {
            unbufferedSource.close();
        }
    }
    private final static class TaskResult {
        private MediaPacket next;
        private TaskResult(MediaPacket next) {
            this.next = next;
        }
        private MediaPacket next() {
            return next;
        }
    }
    private final class PopulateTask implements Runnable {
        private final List<TaskResult> local;
        private PopulateTask(int queueSize) {
            local = new ArrayList<>(queueSize);
        }
        @Override
        public void run() {
            synchronized (unbufferedSource) {
                final int remainingCapacity = taskResultQueue.remainingCapacity();
                for (int i = 0; i < remainingCapacity; i++) {
                    if (unbufferedSource.hasNext()) {
                        local.add(new TaskResult(unbufferedSource.next()));
                    } else {
                        local.add(EOF);
                        break;
                    }
                }
            }
            taskResultQueue.addAll(local);
            local.clear();
        }
    }
}
