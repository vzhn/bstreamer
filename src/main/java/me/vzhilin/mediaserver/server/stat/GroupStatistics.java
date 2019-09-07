package me.vzhilin.mediaserver.server.stat;

public final class GroupStatistics {
    private long totalConnections;
    private long totalBytes;
    private long totalLagCounter;

    private long lagCounter;
    private long byteCounter;
    private long connOpenCounter;
    private long connCloseCounter;

    public GroupStatistics() { }

    public synchronized void incOpenConn() {
        ++totalConnections;
        ++connOpenCounter;
    }

    public synchronized void incCloseConn() {
        --totalConnections;
        ++connCloseCounter;
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
        connOpenCounter = 0;
        connCloseCounter = 0;
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
            this.totalConnections = gs.totalConnections;
            this.totalBytes = gs.totalBytes;
            this.totalLagCounter = gs.totalLagCounter;
            this.lagCounter = gs.lagCounter;
            this.byteCounter = gs.byteCounter;
            this.connOpenCounter = gs.connOpenCounter;
            this.connCloseCOunter = gs.connCloseCounter;
        }
    }
}
