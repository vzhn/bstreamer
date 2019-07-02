package me.vzhilin.mediaserver.server;

import io.netty.channel.EventLoopGroup;
import me.vzhilin.mediaserver.conf.Config;
import me.vzhilin.mediaserver.media.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.media.file.SourceFactoryRegistry;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactory;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactoryRegistry;
import me.vzhilin.mediaserver.server.strategy.sync.SyncStrategyFactory;

public class ServerContext {
    private final ServerStatistics stat;
    private final Config config;
    private EventLoopGroup scheduledExecutor;

    private final SourceFactoryRegistry sourceFactoryRegistry;
    private final StreamingStrategyFactoryRegistry streamingStrategyRegistry;

    public ServerContext(Config config) {
        this.config = config;
        stat = new ServerStatistics();
        sourceFactoryRegistry = new SourceFactoryRegistry();
        streamingStrategyRegistry = new StreamingStrategyFactoryRegistry();
    }

    public ServerStatistics getStat() {
        return stat;
    }

    public void setScheduledExecutor(EventLoopGroup scheduledExecutor) {
        this.scheduledExecutor = scheduledExecutor;
    }

    public EventLoopGroup getScheduledExecutor() {
        return scheduledExecutor;
    }

    public SourceFactoryRegistry getSourceFactoryRegistry() {
        return sourceFactoryRegistry;
    }

    public MediaPacketSourceFactory getSourceFactory(String sourceName) {
        return sourceFactoryRegistry.get(sourceName);
    }

    public Config getConfig() {
        return config;
    }

    public void registerSyncStrategy(String name, SyncStrategyFactory syncStrategyFactory) {
        streamingStrategyRegistry.addFactory(name, syncStrategyFactory);
    }

    public StreamingStrategyFactory getStreamingStrategyFactory(String name) {
        return streamingStrategyRegistry.get(name);
    }

    public void registerSourceFactory(String name, MediaPacketSourceFactory sourceFactory) {
        sourceFactoryRegistry.register(name, sourceFactory);
    }
}
