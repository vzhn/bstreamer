package me.vzhilin.mediaserver.server.strategy.seq;

import io.netty.channel.ChannelHandlerContext;
import me.vzhilin.mediaserver.media.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;

public class SequencedStrategy implements StreamingStrategy {
    @Override
    public void attachContext(ChannelHandlerContext context) {

    }

    @Override
    public void detachContext(ChannelHandlerContext context) {

    }

    @Override
    public MediaPacketSourceDescription describe() {
        return null;
    }
}
