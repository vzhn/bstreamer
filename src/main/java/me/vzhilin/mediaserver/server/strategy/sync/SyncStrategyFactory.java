package me.vzhilin.mediaserver.server.strategy.sync;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.impl.file.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.server.ServerContext;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SyncStrategyFactory implements StreamingStrategyFactory {
    private final Map<EventLoopGroup, Map<PropertyMap, SyncStrategy>> filenameToStrategy = new HashMap<>();
    private final ServerContext context;

    public SyncStrategyFactory(ServerContext context) {
        this.context = context;
    }

    @Override
    public StreamingStrategy getStrategy(EventLoop eventLoop, PropertyMap sourceConfig) {
        synchronized (this) {
            return filenameToStrategy
                    .computeIfAbsent(eventLoop, el -> new HashMap<>())
                    .computeIfAbsent(sourceConfig, sc -> new SyncStrategy(context, eventLoop, context.getSource(sourceConfig)));
        }
    }

    @Override
    public MediaPacketSourceDescription describe(EventLoop eventLoop, PropertyMap sourceConfig) {
        return getStrategy(eventLoop, sourceConfig).describe();
    }
}
