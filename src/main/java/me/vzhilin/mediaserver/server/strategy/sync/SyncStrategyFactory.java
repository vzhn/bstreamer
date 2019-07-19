package me.vzhilin.mediaserver.server.strategy.sync;

import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.file.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.server.ServerContext;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class SyncStrategyFactory implements StreamingStrategyFactory {
    private final Map<PropertyMap, SyncStrategy> filenameToStrategy = new HashMap<>();
    private final ServerContext context;
    private final ExecutorService executor;

    public SyncStrategyFactory(ServerContext context, ExecutorService executor) {
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
