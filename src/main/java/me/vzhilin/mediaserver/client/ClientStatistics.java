package me.vzhilin.mediaserver.client;

public class ClientStatistics {
    long ts = System.currentTimeMillis();
    long total = 0;

    public synchronized void touch(long size) {
        total += size;

        long now = System.currentTimeMillis();
        long delta = now - ts;
        if (delta > 1000) {
            System.err.println(1e-6 * total / delta * 1000f);

            total = 0;
            ts = now;
        }
    }
}
