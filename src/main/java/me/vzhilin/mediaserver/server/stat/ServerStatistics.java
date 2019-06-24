package me.vzhilin.mediaserver.server.stat;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.HTTPServer;

import java.io.IOException;

public final class ServerStatistics {
    private final MetricRegistry registry;
    private final Meter throughputMeter;
    private final Counter lagMillis;

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

    public Meter getThroughputMeter() {
        return throughputMeter;
    }

    public Counter getLagMillis() {
        return lagMillis;
    }
}
