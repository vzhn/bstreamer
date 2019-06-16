package me.vzhilin.mediaserver.media;

import io.netty.buffer.ByteBuf;

public class Packet {
    private final long pts;
    private final long dts;
    private final ByteBuf payload;
    private final boolean key;

    public Packet(long pts, long dts, boolean key, ByteBuf payload) {
        this.pts = pts;
        this.dts = dts;
        this.key = key;
        this.payload = payload;
    }

    public long getPts() {
        return pts;
    }

    public long getDts() {
        return dts;
    }

    public boolean isKey() {
        return key;
    }

    public ByteBuf getPayload() {
        return payload;
    }
}
