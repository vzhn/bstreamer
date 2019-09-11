package me.vzhilin.bstreamer.client;

public class ConnectionStatistics {
    private final TotalStatistics ss;

    private long size;

    public ConnectionStatistics(TotalStatistics ss) {
        this.ss = ss;
    }
    public synchronized void onRead(int bytes) {
        size += bytes;

        ss.onRead(bytes);
    }

    public long getSize() {
        return size;
    }

    public void onConnected() {
        ss.incConnections();
    }

    public void onDisconnected() {
        ss.decConnections();
    }

    public void onIdleError() {
        ss.onIdleError();
    }
}
