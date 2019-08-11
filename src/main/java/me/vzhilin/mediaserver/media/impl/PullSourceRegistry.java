package me.vzhilin.mediaserver.media.impl;

import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.PullSource;
import me.vzhilin.mediaserver.media.impl.file.SourceFactoryRegistry;
import me.vzhilin.mediaserver.server.ServerContext;
import me.vzhilin.mediaserver.util.scheduler.PushSource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

public final class PullSourceRegistry {
    // source name -> source properties -> source
    private final Map<String, Map<PropertyMap, PushSource>> sources = new HashMap<>();
    private final SourceFactoryRegistry factoryRegistry;

    public PullSourceRegistry(SourceFactoryRegistry factoryRegistry) {
        this.factoryRegistry = factoryRegistry;
    }

    public PushSource get(ServerContext ctx, PropertyMap config) {
        String name = config.getValue(CommonSourceAttributes.NAME);
        ScheduledExecutorService workers = ctx.getWorkerExecutors();
        Map<PropertyMap, PushSource> sm = sources.computeIfAbsent(name, n -> new HashMap<>());
        Function<PropertyMap, PushSource> mappingFunction = propertyMap -> {
            PullSourceFactory factory = factoryRegistry.get(name);
            Supplier<PullSource> supplier = () -> factory.newSource(ctx, propertyMap);
            return new PushSource(supplier, config, workers, ctx.getConfig().getBufferingLimits());
        };
        return sm.computeIfAbsent(config, mappingFunction);
    }
}
