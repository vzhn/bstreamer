package me.vzhilin.mediaserver.server.stat;

public final class GroupStatistics {
    private long totalConnections;
    private long totalBytes;
    private long totalLateCounter;

    private long lateCounter;
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

    public synchronized void incLateCount() {
        ++totalLateCounter;
        ++lateCounter;
    }

    public GroupStatisticsSnapshot snapshot() {
        GroupStatisticsSnapshot snapshot = new GroupStatisticsSnapshot(this);
        lateCounter = 0;
        byteCounter = 0;
        connOpenCounter = 0;
        connCloseCounter = 0;
        return snapshot;
    }

    public static class GroupStatisticsSnapshot {
        public final long totalConnections;
        public final long totalBytes;
        public final long totalLateCounter;
        public final long lateCounter;
        public final long byteCounter;
        public final long connOpenCounter;
        public final long connCloseCOunter;

        public GroupStatisticsSnapshot(GroupStatistics gs) {
            this.totalConnections = gs.totalConnections;
            this.totalBytes = gs.totalBytes;
            this.totalLateCounter = gs.totalLateCounter;
            this.lateCounter = gs.lateCounter;
            this.byteCounter = gs.byteCounter;
            this.connOpenCounter = gs.connOpenCounter;
            this.connCloseCOunter = gs.connCloseCounter;
        }
    }
}
