package me.vzhilin.bstreamer.server;

import io.netty.channel.EventLoop;
import me.vzhilin.bstreamer.server.conf.Config;
import me.vzhilin.bstreamer.server.media.impl.PullSourceRegistry;
import me.vzhilin.bstreamer.server.stat.ServerStatistics;
import me.vzhilin.bstreamer.server.strategy.sync.GroupStreamer;
import me.vzhilin.bstreamer.util.PropertyMap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ServerContext {
    private final ServerStatistics stat;
    private final Config config;
    private final PullSourceRegistry pullSourceRegistry;

    private final Map<StreamKey, GroupStreamer> streams = new HashMap<>();

    public ServerContext(Config config) {
        this.config = config;
        this.stat = new ServerStatistics();
        ScheduledExecutorService workerExecutors = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        this.pullSourceRegistry = new PullSourceRegistry(this, config.getBufferingLimits(), workerExecutors);
    }

    public ServerStatistics getStat() {
        return stat;
    }

    public Config getConfig() {
        return config;
    }

    public synchronized GroupStreamer getStreamer(EventLoop eventLoop, String url, String clazz, PropertyMap cfg) {
        return streams.computeIfAbsent(new StreamKey(eventLoop, url, clazz, cfg),
                sk -> new GroupStreamer(ServerContext.this, sk.eventLoop, pullSourceRegistry.get(sk.sourceKey)));
    }
}
