package me.vzhilin.mediaserver.server.stat;

import me.vzhilin.mediaserver.util.metric.PeriodCounter;

public final class GroupStatistics {
    private final PeriodCounter lateCounter = new PeriodCounter();
    private final PeriodCounter byteCounter = new PeriodCounter();
    private final PeriodCounter connOpenCounter = new PeriodCounter();
    private final PeriodCounter connCloseCounter = new PeriodCounter();

    public GroupStatistics() { }

    public void openConn() {
        connOpenCounter.inc(System.currentTimeMillis(), 1);
    }

    public void closeConn() {
        connCloseCounter.inc(System.currentTimeMillis(), 1);
    }

    public void incByteCount(int bytes) {
        byteCounter.inc(System.currentTimeMillis(), bytes);
    }

    public void incLateCount() {
        lateCounter.inc(System.currentTimeMillis(), 1);
    }
}
