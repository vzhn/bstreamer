package me.vzhilin.mediaserver.client;

public class ConnectionStatistics {
    private long initTs;
    private long connectedTs;
    private long disconnectedTs;
    private long size;

    public void onConnected() {
        connectedTs = System.currentTimeMillis();
    }

    public void onDisconnected() {
        disconnectedTs = System.currentTimeMillis();
    }

    public void onRead(int bytes) {
        size += bytes;
    }

    public long getTotal() {
        return size;
    }
}
