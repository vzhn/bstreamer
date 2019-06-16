package me.vzhilin.mediaserver.server.strategy;

import io.netty.channel.ChannelHandlerContext;

public interface StreamingStrategy {
    void attachContext(ChannelHandlerContext context);
    void detachContext(ChannelHandlerContext context);
}
