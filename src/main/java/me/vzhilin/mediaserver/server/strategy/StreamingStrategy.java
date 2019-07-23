package me.vzhilin.mediaserver.server.strategy;

import io.netty.channel.ChannelHandlerContext;
import me.vzhilin.mediaserver.media.impl.file.MediaPacketSourceDescription;

public interface StreamingStrategy {
    void attachContext(ChannelHandlerContext context);
    void detachContext(ChannelHandlerContext context);

    MediaPacketSourceDescription describe();
}
