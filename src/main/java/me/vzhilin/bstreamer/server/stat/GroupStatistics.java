package me.vzhilin.bstreamer.server.stat;

public final class GroupStatistics {
    private long connections;
    private long openedConnections;
    private long closedConnections;

    private long totalLagCounter;
    private long lagCounter;

    private long totalBytes;
    private long byteCounter;

    public GroupStatistics() { }

    public synchronized long connections() {
        return connections;
    }

    public synchronized void incOpenConn() {
        ++connections;
        ++openedConnections;
    }

    public synchronized void incCloseConn() {
        --connections;
        ++closedConnections;
    }

    public synchronized void incByteCount(long bytes) {
        totalBytes += bytes;
        byteCounter += bytes;
    }

    public synchronized void incLagCount() {
        ++totalLagCounter;
        ++lagCounter;
    }

    public GroupStatisticsSnapshot snapshot() {
        GroupStatisticsSnapshot snapshot = new GroupStatisticsSnapshot(this);
        lagCounter = 0;
        byteCounter = 0;
        openedConnections = 0;
        closedConnections = 0;
        return snapshot;
    }

    public static class GroupStatisticsSnapshot {
        public final long totalConnections;
        public final long totalBytes;
        public final long totalLagCounter;
        public final long lagCounter;
        public final long byteCounter;
        public final long connOpenCounter;
        public final long connCloseCOunter;

        public GroupStatisticsSnapshot(GroupStatistics gs) {
            this.totalConnections = gs.connections;
            this.totalBytes = gs.totalBytes;
            this.totalLagCounter = gs.totalLagCounter;
            this.lagCounter = gs.lagCounter;
            this.byteCounter = gs.byteCounter;
            this.connOpenCounter = gs.openedConnections;
            this.connCloseCOunter = gs.closedConnections;
        }
    }
}
