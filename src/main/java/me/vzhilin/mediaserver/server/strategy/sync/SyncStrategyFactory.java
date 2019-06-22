package me.vzhilin.mediaserver.server.strategy.sync;

import me.vzhilin.mediaserver.media.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.media.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

public class SyncStrategyFactory implements StreamingStrategyFactory {
    private final Map<MediaPacketSourceFactory, SyncStrategy> filenameToStrategy = new HashMap<>();
    private final ScheduledExecutorService scheduledExecutor;

    public SyncStrategyFactory(ScheduledExecutorService scheduledExecutor) {
        this.scheduledExecutor = scheduledExecutor;
    }

    @Override
    public StreamingStrategy getStrategy(MediaPacketSourceFactory sourceFactory) {
        return filenameToStrategy.computeIfAbsent(sourceFactory, s -> new SyncStrategy(s, scheduledExecutor));
    }

    @Override
    public MediaPacketSourceDescription describe(MediaPacketSourceFactory sourceFactory) {
        return getStrategy(sourceFactory).describe();
    }
}
