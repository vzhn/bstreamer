package me.vzhilin.bstreamer.client;

import me.vzhilin.bstreamer.util.HumanReadable;
import me.vzhilin.bstreamer.util.ReporterWriter;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ClientReporter {
    private final TotalStatistics stat;
    private final ReporterWriter reporterWriter;

    public ClientReporter(TotalStatistics stat) {
        this.stat = stat;

        this.reporterWriter = new ReporterWriter(
                new ReporterWriter.Column("time", 8),
                new ReporterWriter.Column("server connections", 20),
                new ReporterWriter.Column("errors", 11),
                new ReporterWriter.Column("throughput", 11)
        );
        reporterWriter.writeHeader(System.out);
    }

    public void start() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new ReporterTask(), 1, 1, TimeUnit.SECONDS);
    }

    private final class ReporterTask implements Runnable {
        @Override
        public void run() {
            TotalStatistics.Snapshot s = stat.snapshot();
            long gbps;
            if (s.deltaTime == 0) {
                gbps = 0;
            } else {
                gbps = 8 * s.bytes / s.deltaTime * 1000;
            }
            String bandwidth = HumanReadable.humanReadableByteCount(gbps, false);
            LocalDateTime now = LocalDateTime.now();

            String time = String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond());
            String connections = String.format("%d [+%d:-%d]", s.connections, s.connected, s.disconnected);
            String errors = String.format("%d [+%d]", s.totalErrors, s.errors);
            reporterWriter.writeLine(System.out, time, connections, errors, bandwidth);
        }
    }
}
