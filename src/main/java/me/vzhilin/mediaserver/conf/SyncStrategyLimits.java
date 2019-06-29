package me.vzhilin.mediaserver.conf;

public class SyncStrategyLimits {
    private final int packets;
    private final int size;
    private final int time;

    public SyncStrategyLimits(int packets, int size, int time) {
        this.packets = packets;
        this.size = size;
        this.time = time;
    }

    public int getPackets() {
        return packets;
    }

    public int getSize() {
        return size;
    }

    public int getTime() {
        return time;
    }
}
