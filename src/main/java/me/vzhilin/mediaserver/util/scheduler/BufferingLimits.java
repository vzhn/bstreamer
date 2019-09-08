package me.vzhilin.mediaserver.util.scheduler;

public final class BufferingLimits {
    private final long bytes;
    private final int npackets;
    private final long timeMillis;

    public BufferingLimits(long bytes, int npackets, long timeMillis) {
        this.bytes = bytes;
        this.npackets = npackets;
        this.timeMillis = timeMillis;
    }

    public boolean check(long bytes, long npackets, long timeMillis) {
        return (this.bytes == 0 || bytes < this.bytes) &&
               (this.npackets == 0 || npackets < this.npackets) &&
               timeMillis < this.timeMillis;
    }
}
