package me.vzhilin.mediaserver.server.stat;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public final class ServerStatistics {
    private final MetricRegistry registry;
    private final Meter throughputMeter;
    private final Counter lagMillis;

    public ServerStatistics() {
        MetricRegistry registry = new MetricRegistry();
        this.registry = registry;
        this.throughputMeter = registry.meter("throughput");
        this.lagMillis = registry.counter("lagMillis");
    }

    public Meter getThroughputMeter() {
        return throughputMeter;
    }

    public Counter getLagMillis() {
        return lagMillis;
    }
}
