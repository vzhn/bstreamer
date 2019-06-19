package me.vzhilin.mediaserver.client;

import java.util.HashSet;
import java.util.Set;

public class TotalStatistics {
    private Set<ConnectionStatistics> stats = new HashSet<>();
    private long sz;
    private long tsStart;
    private long tsEnd;

    public void onStart() {
        tsStart = System.currentTimeMillis();
    }

    public void onShutdown() {
        tsEnd = System.currentTimeMillis();
    }

    public synchronized void onRead(int bytes) {
        sz += bytes;
    }

    public synchronized long getSize() {
        return sz;
    }

    public ConnectionStatistics newStat() {
        ConnectionStatistics s = new ConnectionStatistics(this);
        stats.add(s);
        return s;
    }

    @Override
    public String toString() {
        long deltaMillis = tsEnd - tsStart;

        float mbps = 1e-9f * 8 * sz / deltaMillis * 1000;
        return String.format("TotalStatistics{gbps=%.2f}", mbps);
    }

    public Snapshot snapshot() {
        return new Snapshot(sz);
    }

    public final static class Snapshot {
        private final long sz;
        private final long timestamp;

        public Snapshot(long sz) {
            this.sz = sz;
            this.timestamp = System.currentTimeMillis();
        }

        public Diff diff(Snapshot prev) {
            return new Diff(sz - prev.sz, timestamp - prev.timestamp);
        }
    }

    public final static class Diff {
        private final long deltaSize;
        private final long deltaTime;
        private final float gbps;

        public Diff(long deltaSize, long deltaTime) {
            this.deltaSize = deltaSize;
            this.deltaTime = deltaTime;

            gbps = 1e-9f * 8 * deltaSize / deltaTime * 1000;

        }

        @Override
        public String toString() {
            return "Diff{" +
                    "deltaSize=" + deltaSize +
                    ", deltaTime=" + deltaTime +
                    ", gbps=" + gbps +
                    '}';
        }
    }
}
