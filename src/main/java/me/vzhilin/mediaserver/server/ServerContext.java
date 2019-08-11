package me.vzhilin.mediaserver.server;

import io.netty.channel.EventLoopGroup;
import me.vzhilin.mediaserver.conf.Config;
import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.impl.PullSourceFactory;
import me.vzhilin.mediaserver.media.impl.PullSourceRegistry;
import me.vzhilin.mediaserver.media.impl.file.SourceFactoryRegistry;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactory;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactoryRegistry;
import me.vzhilin.mediaserver.server.strategy.sync.SyncStrategyFactory;
import me.vzhilin.mediaserver.util.scheduler.PushSource;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ServerContext {
    private final ServerStatistics stat;
    private final Config config;
    private final SourceFactoryRegistry sourceRegistry;
    private final PullSourceRegistry pullSourceRegistry;
    private ScheduledExecutorService scheduledExecutor;

    private final StreamingStrategyFactoryRegistry streamingStrategyRegistry;
    private final ScheduledExecutorService workerExecutors =
        Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    public ServerContext(Config config) {
        this.config = config;
        stat = new ServerStatistics();
        sourceRegistry = new SourceFactoryRegistry();
        pullSourceRegistry = new PullSourceRegistry(sourceRegistry);
        streamingStrategyRegistry = new StreamingStrategyFactoryRegistry();
    }

    public ServerStatistics getStat() {
        return stat;
    }

    public void setScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
        this.scheduledExecutor = scheduledExecutor;
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
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

    public void registerSourceFactory(String name, PullSourceFactory sourceFactory) {
        sourceRegistry.register(name, sourceFactory);
    }

    public PushSource getSource(PropertyMap props) {
        return pullSourceRegistry.get(this, props);
    }

    public ScheduledExecutorService getWorkerExecutors() {
        return workerExecutors;
    }
}
