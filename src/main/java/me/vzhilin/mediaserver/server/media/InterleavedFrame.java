package me.vzhilin.mediaserver.server.media;

import io.netty.buffer.ByteBuf;

public final class InterleavedFrame {
    private ByteBuf buffer;

    public InterleavedFrame(ByteBuf buffer) {
        this.buffer = buffer;
    }

    public ByteBuf getPayload() {
        return buffer;
    }

    public int getSize() {
        return buffer.readableBytes();
    }

    public void release() {
        buffer.release();
    }

    public void retain(int count) {
        if (count != 0) {
            buffer.retain(count);
        }
    }
}
