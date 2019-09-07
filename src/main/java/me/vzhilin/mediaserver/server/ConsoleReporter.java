package me.vzhilin.mediaserver.server;

import me.vzhilin.mediaserver.server.stat.GroupStatistics;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;
import me.vzhilin.mediaserver.util.HumanReadable;
import me.vzhilin.mediaserver.util.metric.PeriodCounter;

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
        private final PeriodCounter connOpen;
        private final PeriodCounter connClose;
        private final PeriodCounter lag;
        private final PeriodCounter bytes;

        public Reporter(GroupStatistics stat) {
            this.connOpen = stat.getConnOpen();
            this.connClose = stat.getConnClose();
            this.lag = stat.getLate();
            this.bytes = stat.getBytes();
        }

        @Override
        public void run() {
            StringBuilder sb = new StringBuilder();

            long c = connOpen.total() - connClose.total();
            long op = getLastSecond(connOpen);
            long cl = getLastSecond(connClose);

            long lagMinute = lag.getHour().getLastSampleCount();
            long lagHour = lag.getDay().getLastSampleCount();
            long lagDay = lag.getWeek().getLastSampleCount();
            long lagTotal = lag.total();

            long bytesTotal = bytes.getMinute().getLastEntireSampleCount();
            String sBytes = HumanReadable.humanReadableByteCount(bytesTotal, true);

            LocalDateTime now = LocalDateTime.now();

            String mesg = String.format("\r%02d:%02d:%02d C %d [+%d:-%d] L %d [%d:%d:%d] %s ",
                    now.getHour(), now.getMinute(), now.getSecond(),
                    c, op, cl, lagTotal, lagMinute, lagHour, lagDay, sBytes);
            sb.append(mesg);
            System.out.print(sb);
        }

        private long getLastSecond(PeriodCounter pc) {
            return pc.getMinute().getLastEntireSampleCount();
        }
    }
}
