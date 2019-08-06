package me.vzhilin.mediaserver.client;

public class ConnectionStatistics {
    private final TotalStatistics ss;

    private long initTs;
    private long connectedTs;
    private long disconnectedTs;
    private long size;

    public ConnectionStatistics(TotalStatistics ss) {
        this.ss = ss;
    }

    public void onConnected() {
        connectedTs = System.currentTimeMillis();
    }

    public void onDisconnected() {
        disconnectedTs = System.currentTimeMillis();
    }

    public synchronized void onRead(int bytes) {
        size += bytes;

        ss.onRead(bytes);
    }

    public long getSize() {
        return size;
    }

    @Override
    public synchronized String toString() {
        return "ConnectionStatistics{" +
                "size=" + size +
                '}';
    }
}
