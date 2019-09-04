package me.vzhilin.mediaserver.client;

import me.vzhilin.mediaserver.util.HumanReadable;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
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
        @Override
        public void run() {
            TotalStatistics.Snapshot s = stat.snapshot();
            String direct = HumanReadable.humanReadableByteCount(directMemoryPool.getMemoryUsed(), false);

            long gbps = 8 * s.bytes / s.deltaTime * 1000;
            String bandwidth = HumanReadable.humanReadableByteCount(gbps, false);
            LocalDateTime now = LocalDateTime.now();
            System.out.printf("\r%02d:%02d:%02d %d [+%d; -%d] %s ",
                    now.getHour(), now.getMinute(), now.getSecond(),
                    s.connections, s.connected, s.disconnected, bandwidth);
        }
    }
}
