package me.vzhilin.bstreamer.server.strategy;

import io.netty.channel.EventLoop;
import me.vzhilin.bstreamer.util.PropertyMap;

public interface StreamingStrategyFactory {
//    StreamingStrategy getStrategy(EventLoop loopGroup, PropertyMap sourceConfig);

    StreamingStrategy getStrategy(EventLoop loop, String clazz, PropertyMap conf);
}
