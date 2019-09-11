package me.vzhilin.bstreamer.server;

import me.vzhilin.bstreamer.server.stat.GroupStatistics;
import me.vzhilin.bstreamer.server.stat.ServerStatistics;
import me.vzhilin.bstreamer.util.HumanReadable;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ConsoleReporter {
    private final ScheduledExecutorService exec;
    private Runnable reporter;
    private ScheduledFuture<?> reporterFuture;

    public ConsoleReporter(ServerStatistics stat, ScheduledExecutorService exec) {
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

        public Reporter(GroupStatistics stat) {
            this.stat = stat;
        }

        @Override
        public void run() {
            StringBuilder sb = new StringBuilder();
            GroupStatistics.GroupStatisticsSnapshot snap = stat.snapshot();
            long c = snap.totalConnections;
            long op = snap.connOpenCounter;
            long cl = snap.connCloseCOunter;
            long lagSecond = snap.lagCounter;
            long lagTotal = snap.totalLagCounter;
            String sBytes = HumanReadable.humanReadableByteCount(snap.byteCounter, true);
            LocalDateTime now = LocalDateTime.now();
            String mesg = String.format("\r%02d:%02d:%02d C %d [+%d:-%d] L %d [+%d] %s ",
                    now.getHour(), now.getMinute(), now.getSecond(),
                    c, op, cl, lagTotal, lagSecond, sBytes);
            sb.append(mesg);
            System.out.print(sb);
        }
    }
}
