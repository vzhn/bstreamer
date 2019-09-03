package me.vzhilin.mediaserver.client;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class TotalStatistics {
    private Set<ConnectionStatistics> stats = new HashSet<>();
    private long totalBytes;
    private long totalConnections;

    private long time = System.currentTimeMillis();
    private long bytes;
    private int connected;
    private int disconnected;

    public synchronized void onRead(int bytes) {
        this.totalBytes += bytes;
        this.bytes += bytes;
    }

    public synchronized long getSize() {
        return totalBytes;
    }

    public ConnectionStatistics newStat() {
        ConnectionStatistics s = new ConnectionStatistics(this);
        stats.add(s);
        return s;
    }

    public synchronized Snapshot snapshot() {
        long now = System.currentTimeMillis();
        long deltaTime = now - time;
        Snapshot snapshot = new Snapshot(this, deltaTime);
        this.bytes = 0;
        this.connected = 0;
        this.disconnected = 0;
        this.time = now;
        return snapshot;
    }

    public synchronized void incConnections() {
        ++connected;
        ++totalConnections;
    }

    public synchronized void decConnections() {
        ++disconnected;
        --totalConnections;
    }

    public final static class Snapshot {
        public final long totalBytes;
        public final long connections;
        public final long bytes;
        public final int connected;
        public final int disconnected;
        public final Date time;
        public final long deltaTime;

        public Snapshot(TotalStatistics totalStatistics, long deltaTime) {
            this.totalBytes = totalStatistics.totalBytes;
            this.connections = totalStatistics.totalConnections;
            this.bytes = totalStatistics.bytes;
            this.connected = totalStatistics.connected;
            this.disconnected = totalStatistics.disconnected;
            this.deltaTime = deltaTime;
            this.time = new Date();
        }
    }
}
