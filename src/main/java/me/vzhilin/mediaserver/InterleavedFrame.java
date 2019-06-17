package me.vzhilin.mediaserver;

import io.netty.buffer.ByteBuf;

public final class InterleavedFrame {
    private ByteBuf buffer;

    public InterleavedFrame(ByteBuf buffer) {
        this.buffer = buffer;
    }

    public ByteBuf getPayload() {
        return buffer;
    }
}
