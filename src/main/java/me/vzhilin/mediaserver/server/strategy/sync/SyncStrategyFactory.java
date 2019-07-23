package me.vzhilin.mediaserver.server.strategy.sync;

import io.netty.channel.EventLoopGroup;
import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.impl.file.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.server.ServerContext;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactory;

import java.util.HashMap;
import java.util.Map;

public class SyncStrategyFactory implements StreamingStrategyFactory {
    private final Map<PropertyMap, SyncStrategy> filenameToStrategy = new HashMap<>();
    private final ServerContext context;
    private final EventLoopGroup executor;

    public SyncStrategyFactory(ServerContext context, EventLoopGroup executor) {
        this.context = context;
        this.executor = executor;
    }

    @Override
    public StreamingStrategy getStrategy(PropertyMap sourceConfig) {
        return filenameToStrategy.computeIfAbsent(sourceConfig, s -> new SyncStrategy(context, executor, sourceConfig));
    }

    @Override
    public MediaPacketSourceDescription describe(PropertyMap sourceConfig) {
        return getStrategy(sourceConfig).describe();
    }
}
