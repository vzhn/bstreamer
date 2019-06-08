package me.vzhilin.mediaserver.client;

import io.netty.buffer.ByteBuf;

public class InterleavedPacket {
    private final int channel;
    private final ByteBuf payload;

    public InterleavedPacket(int channel, ByteBuf payload) {
        this.channel = channel;
        this.payload = payload;
    }

    public int getChannel() {
        return channel;
    }

    public ByteBuf getPayload() {
        return payload;
    }
}
