package me.vzhilin.mediaserver.server.strategy.sync;

import me.vzhilin.mediaserver.conf.Config;
import me.vzhilin.mediaserver.media.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.media.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

public class SyncStrategyFactory implements StreamingStrategyFactory {
    private final Map<MediaPacketSourceFactory, SyncStrategy> filenameToStrategy = new HashMap<>();
    private final ScheduledExecutorService scheduledExecutor;
    private final ServerStatistics stat;
    private final Config config;

    public SyncStrategyFactory(ScheduledExecutorService scheduledExecutor, ServerStatistics stat, Config config) {
        this.scheduledExecutor = scheduledExecutor;
        this.stat = stat;
        this.config = config;
    }

    @Override
    public StreamingStrategy getStrategy(MediaPacketSourceFactory sf) {
        return filenameToStrategy.computeIfAbsent(sf, s -> new SyncStrategy(s, scheduledExecutor, stat, config));
    }

    @Override
    public MediaPacketSourceDescription describe(MediaPacketSourceFactory sourceFactory) {
        return getStrategy(sourceFactory).describe();
    }
}
