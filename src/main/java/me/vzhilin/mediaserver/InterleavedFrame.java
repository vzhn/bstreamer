package me.vzhilin.mediaserver;

import io.netty.buffer.ByteBuf;

public class InterleavedFrame {
    private ByteBuf buffer;

    public InterleavedFrame(ByteBuf buffer) {
        this.buffer = buffer;
    }

    public ByteBuf getPayload() {
        return buffer;
    }

    public void setSeqNo(int i) {

    }
}
