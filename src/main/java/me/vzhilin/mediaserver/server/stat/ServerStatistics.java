package me.vzhilin.mediaserver.server.stat;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.HTTPServer;
import me.vzhilin.mediaserver.conf.PropertyMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class ServerStatistics {
    private final Map<PropertyMap, GroupStatistics> groupStats = new HashMap<>();

    private final MetricRegistry registry;
    private final Meter throughputMeter;
    private final Counter lagMillis;

    private int clientCount;
    private int groupCount;

    public ServerStatistics() {
        MetricRegistry registry = new MetricRegistry();
        this.registry = registry;
        this.throughputMeter = registry.meter("throughput");
        this.lagMillis = registry.counter("lagMillis");

        CollectorRegistry.defaultRegistry.register(new DropwizardExports(registry));
        try {
            HTTPServer server = new HTTPServer(1234);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
//                .convertRatesTo(TimeUnit.SECONDS)
//                .convertDurationsTo(TimeUnit.MILLISECONDS)
//                .build();
//        reporter.start(1, TimeUnit.SECONDS);
    }

    public synchronized GroupStatistics getGroupStatistics(PropertyMap properties) {
        return groupStats.get(properties);
    }

    public synchronized GroupStatistics addGroupStatistics(PropertyMap properties) {
        if (!groupStats.containsKey(properties)) {
            ++groupCount;
            GroupStatistics groupStatistics = new GroupStatistics(this);
            groupStats.put(properties, groupStatistics);
            return groupStatistics;
        } else {
            return groupStats.get(properties);
        }

    }

    public synchronized void removeGroupStatistics(PropertyMap properties) {
        --groupCount;
        groupStats.remove(properties);
    }

    public Meter getThroughputMeter() {
        return throughputMeter;
    }

    public Counter getLagMillis() {
        return lagMillis;
    }

    public synchronized int getClientCount() {
        return clientCount;
    }

    public synchronized int getGroupCount() {
        return groupCount;
    }

    public synchronized void incClientCount() {
        ++clientCount;
    }

    public synchronized void decClientCount() {
        --clientCount;
    }
}
