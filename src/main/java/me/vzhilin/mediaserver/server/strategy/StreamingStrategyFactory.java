package me.vzhilin.mediaserver.server.strategy;

import io.netty.channel.EventLoop;
import me.vzhilin.mediaserver.conf.PropertyMap;

public interface StreamingStrategyFactory {
//    StreamingStrategy getStrategy(EventLoop loopGroup, PropertyMap sourceConfig);

    StreamingStrategy getStrategy(EventLoop loop, String clazz, PropertyMap conf);
}
