package me.vzhilin.bstreamer.server;

import me.vzhilin.bstreamer.server.stat.GroupStatistics;
import me.vzhilin.bstreamer.server.stat.ServerStatistics;
import me.vzhilin.bstreamer.util.HumanReadable;
import me.vzhilin.bstreamer.util.ReporterWriter;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ServerReporter {
    private final ScheduledExecutorService exec;
    private final Runnable reporter;
    private ScheduledFuture<?> reporterFuture;

    public ServerReporter(ServerStatistics stat, ScheduledExecutorService exec) {
        this.exec = exec;
        reporter = new Reporter(stat.getTotal());
    }

    public void start() {
        reporterFuture = exec.scheduleAtFixedRate(reporter, 1, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        reporterFuture.cancel(false);
    }

    private final static class Reporter implements Runnable {
        private final GroupStatistics stat;
        private final ReporterWriter reporterWriter;

        private Reporter(GroupStatistics stat) {
            this.stat = stat;
            this.reporterWriter = new ReporterWriter(
                new ReporterWriter.Column("time", 8),
                new ReporterWriter.Column("group", 8),
                new ReporterWriter.Column("client connections", 20),
                new ReporterWriter.Column("lag", 11),
                new ReporterWriter.Column("throughput", 11)
            );

            reporterWriter.writeHeader(System.out);
        }

        @Override
        public void run() {
            GroupStatistics.GroupStatisticsSnapshot snap = stat.snapshot();
            long c = snap.totalConnections;
            long op = snap.connOpenCounter;
            long cl = snap.connCloseCOunter;
            long lagSecond = snap.lagCounter;
            long lagTotal = snap.totalLagCounter;
            String sBytes = HumanReadable.humanReadableByteCount(8 * snap.byteCounter, true);
            LocalDateTime now = LocalDateTime.now();

            String time = String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond());
            String connections = String.format("%d [+%d:-%d]", c, op, cl);
            String lag = String.format("%d [+%d]", lagTotal, lagSecond);
            reporterWriter.writeLine(System.out, time, connections, lag, sBytes);
        }
    }
}
