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
        return bytes < this.bytes &&
               npackets < this.npackets &&
               timeMillis < this.timeMillis;
    }
}
