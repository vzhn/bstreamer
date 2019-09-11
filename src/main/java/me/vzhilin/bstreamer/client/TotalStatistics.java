package me.vzhilin.bstreamer.client;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class TotalStatistics {
    private Set<ConnectionStatistics> stats = new HashSet<>();
    private long totalBytes;
    private long totalConnections;
    private long totalErrors;

    private long time = System.currentTimeMillis();
    private long bytes;
    private long errors;
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
        this.errors = 0;
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

    public synchronized void onIdleError() {
        ++errors;
        ++totalErrors;
    }

    public final static class Snapshot {
        public final long totalBytes;
        public final long totalErrors;
        public final long connections;
        public final long bytes;
        public final int connected;
        public final int disconnected;
        public final Date time;
        public final long deltaTime;
        public final long errors;

        public Snapshot(TotalStatistics totalStatistics, long deltaTime) {
            this.totalBytes = totalStatistics.totalBytes;
            this.totalErrors = totalStatistics.totalErrors;
            this.connections = totalStatistics.totalConnections;
            this.bytes = totalStatistics.bytes;
            this.errors = totalStatistics.errors;
            this.connected = totalStatistics.connected;
            this.disconnected = totalStatistics.disconnected;
            this.deltaTime = deltaTime;
            this.time = new Date();
        }
    }
}
