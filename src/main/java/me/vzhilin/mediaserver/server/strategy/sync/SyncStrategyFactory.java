package me.vzhilin.mediaserver.server.strategy.sync;

import me.vzhilin.mediaserver.conf.Config;
import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.CommonSourceAttributes;
import me.vzhilin.mediaserver.media.file.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.media.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.media.MediaPaketSourceConfig;
import me.vzhilin.mediaserver.media.file.SourceFactoryRegistry;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

public class SyncStrategyFactory implements StreamingStrategyFactory {
    private final Map<PropertyMap, SyncStrategy> filenameToStrategy = new HashMap<>();
    private final ScheduledExecutorService scheduledExecutor;
    private final ServerStatistics stat;
    private final Config config;
    private final SourceFactoryRegistry sourceFactoryRegistry;

    public SyncStrategyFactory(ScheduledExecutorService scheduledExecutor,
                               ServerStatistics stat,
                               Config config,
                               SourceFactoryRegistry sourceFactoryRegistry) {

        this.scheduledExecutor = scheduledExecutor;
        this.stat = stat;
        this.config = config;
        this.sourceFactoryRegistry = sourceFactoryRegistry;
    }

    @Override
    public StreamingStrategy getStrategy(PropertyMap sourceConfig) {
        String sourceName = sourceConfig.getValue(CommonSourceAttributes.NAME);
        MediaPacketSourceFactory factory = sourceFactoryRegistry.get(sourceName);
        return filenameToStrategy.computeIfAbsent(sourceConfig, s -> new SyncStrategy(factory, sourceConfig, scheduledExecutor, stat, config));
    }

    @Override
    public MediaPacketSourceDescription describe(PropertyMap sourceConfig) {
        return getStrategy(sourceConfig).describe();
    }
}
