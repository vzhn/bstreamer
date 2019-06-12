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
}
