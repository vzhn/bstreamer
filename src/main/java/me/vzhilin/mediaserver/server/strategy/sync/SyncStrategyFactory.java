package me.vzhilin.mediaserver.server.strategy.sync;

import me.vzhilin.mediaserver.media.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

public class SyncStrategyFactory implements StreamingStrategyFactory {
    private final Map<String, SyncStrategy> filenameToStrategy = new HashMap<>();
    private final ScheduledExecutorService scheduledExecutor;

    public SyncStrategyFactory(ScheduledExecutorService scheduledExecutor) {
        this.scheduledExecutor = scheduledExecutor;
    }

    @Override
    public StreamingStrategy getStrategy(String fileName) {
        return filenameToStrategy.computeIfAbsent(fileName, s -> new SyncStrategy(s, scheduledExecutor));
    }

    @Override
    public MediaPacketSourceDescription describe(String fileName) {
        return getStrategy(fileName).describe();
    }
}
