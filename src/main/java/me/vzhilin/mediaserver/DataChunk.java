package me.vzhilin.mediaserver;

import io.netty.buffer.ByteBuf;

public class DataChunk {
    private ByteBuf buffer;

    public DataChunk(ByteBuf buffer) {
        this.buffer = buffer;
    }

    public ByteBuf getBuffer() {
        return buffer;
    }
}
