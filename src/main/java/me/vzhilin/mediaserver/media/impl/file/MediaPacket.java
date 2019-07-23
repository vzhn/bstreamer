package me.vzhilin.mediaserver.media.impl.file;

import io.netty.buffer.ByteBuf;

public class MediaPacket {
    private final long pts;
    private final long dts;
    private final ByteBuf payload;
    private final boolean isKey;

    public MediaPacket(long pts, long dts, boolean isKey, ByteBuf payload) {
        this.pts = pts;
        this.dts = dts;
        this.isKey = isKey;
        this.payload = payload;
    }

    public int size() {
        return payload.readableBytes();
    }

    public boolean isKey() {
        return isKey;
    }

    public ByteBuf getPayload() {
        return payload;
    }

    public long getPts() {
        return pts;
    }

    @Override
    public String toString() {
        return "MediaPacket{" +
                "pts=" + pts +
                ", dts=" + dts +
                ", payload=" + payload +
                '}';
    }

    public long getDts() {
        return dts;
    }
}
