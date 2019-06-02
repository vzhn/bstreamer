package me.vzhilin.mediaserver;

import io.netty.buffer.ByteBuf;

public final class InterleavedFrame {
    private long pts;
    private ByteBuf buffer;

    public InterleavedFrame(long pts, ByteBuf buffer) {
        this.pts = pts;
        this.buffer = buffer;
    }

    public ByteBuf getPayload() {
        return buffer;
    }

    public long getPtsMillis() {
        return pts;
    }

    public void setSeqNo(int i) {
        buffer.setShort(6, i);
    }

    public void setPtsMillis(long ptsMillis) {
        this.pts = ptsMillis;
    }
}
