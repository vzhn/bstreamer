package me.vzhilin.mediaserver.server.stat;

import me.vzhilin.mediaserver.util.metric.PeriodCounter;

public final class GroupStatistics {
    private final PeriodCounter lateCounter = new PeriodCounter();
    private final PeriodCounter byteCounter = new PeriodCounter();
    private final PeriodCounter connOpenCounter = new PeriodCounter();
    private final PeriodCounter connCloseCounter = new PeriodCounter();

    public GroupStatistics() { }

    public void incOpenConn() {
        connOpenCounter.inc(System.currentTimeMillis(), 1);
    }

    public void incCloseConn() {
        connCloseCounter.inc(System.currentTimeMillis(), 1);
    }

    public void incByteCount(int bytes) {
        byteCounter.inc(System.currentTimeMillis(), bytes);
    }

    public void incLateCount() {
        lateCounter.inc(System.currentTimeMillis(), 1);
    }

    public PeriodCounter getLate() {
        return lateCounter;
    }

    public PeriodCounter getBytes() {
        return byteCounter;
    }

    public PeriodCounter getConnOpen() {
        return connOpenCounter;
    }

    public PeriodCounter getConnClose() {
        return connCloseCounter;
    }
}
