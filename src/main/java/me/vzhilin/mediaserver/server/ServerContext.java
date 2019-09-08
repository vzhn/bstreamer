package me.vzhilin.mediaserver.server;

import io.netty.channel.EventLoop;
import me.vzhilin.mediaserver.conf.Config;
import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.impl.PullSourceRegistry;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;
import me.vzhilin.mediaserver.server.strategy.sync.GroupStreamer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ServerContext {
    private final ServerStatistics stat;
    private final Config config;
    private final PullSourceRegistry pullSourceRegistry;
    private ScheduledExecutorService scheduledExecutor;

    private final Map<StreamKey, GroupStreamer> streams = new HashMap<>();

    private final ScheduledExecutorService workerExecutors =
        Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    public ServerContext(Config config) {
        this.config = config;
        stat = new ServerStatistics();
        pullSourceRegistry = new PullSourceRegistry(config.getBufferingLimits(), workerExecutors);
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

    public GroupStreamer getStreamer(EventLoop eventLoop, String url, String clazz, PropertyMap cfg) {
        return streams.computeIfAbsent(new StreamKey(eventLoop, url, clazz, cfg),
                sk -> new GroupStreamer(ServerContext.this, sk.eventLoop, pullSourceRegistry.get(sk.sourceKey)));
    }
}
