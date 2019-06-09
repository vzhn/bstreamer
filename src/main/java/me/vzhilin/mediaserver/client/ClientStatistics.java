package me.vzhilin.mediaserver.client;

public class ClientStatistics {
    private long ts = System.currentTimeMillis();
    private long total = 0;

    public synchronized void touch(long size) {
        total += size;

        long now = System.currentTimeMillis();
        long delta = now - ts;
        if (delta > 1000) {
            System.err.println(8 * 1e-6 * total / delta * 1000f);

            total = 0;
            ts = now;
        }
    }

    public long getTotal() {
        return total;
    }
}
