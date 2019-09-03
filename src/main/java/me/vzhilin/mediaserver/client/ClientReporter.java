package me.vzhilin.mediaserver.client;

import me.vzhilin.mediaserver.util.HumanReadable;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ClientReporter {
    private final TotalStatistics stat;
    private final BufferPoolMXBean directMemoryPool;

    ClientReporter(TotalStatistics stat) {
        this.stat = stat;
        this.directMemoryPool = getDirectMemoryPool();
    }

    public void start() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new ReporterTask(), 1, 1, TimeUnit.SECONDS);
    }

    private BufferPoolMXBean getDirectMemoryPool() {
        List<BufferPoolMXBean> pools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
        for (BufferPoolMXBean pool : pools) {
            if ("mapped".equals(pool.getName())) {
                continue;
            }
            return pool;
        }
        return null;
    }

    private final class ReporterTask implements Runnable {
        private TotalStatistics.Snapshot prev = stat.snapshot();

        @Override
        public void run() {
            TotalStatistics.Snapshot s = stat.snapshot();
            long directMemoryUsed = directMemoryPool.getMemoryUsed();
            String cap = HumanReadable.humanReadableByteCount(directMemoryUsed, false);
            System.err.print("\r" + stat.getSize() + " " + s.diff(prev) + " " + cap);
            prev = s;
        }
    }
}
