package me.vzhilin.mediaserver.server.strategy;

import io.netty.channel.ChannelHandlerContext;
import me.vzhilin.mediaserver.media.MediaPacketSourceDescription;

public interface StreamingStrategy {
    void attachContext(ChannelHandlerContext context);
    void detachContext(ChannelHandlerContext context);

    MediaPacketSourceDescription describe();
}
