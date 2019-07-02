package me.vzhilin.mediaserver.server.stat;

public final class GroupStatistics {
    private final ServerStatistics serverStatistics;
    private int clientCount;

    public GroupStatistics(ServerStatistics serverStatistics) {
        this.serverStatistics = serverStatistics;
    }

    public int getClientCount() {
        return clientCount;
    }

    public void incClientCount() {
        serverStatistics.incClientCount();
        ++clientCount;

    }

    public void decClientCount() {
        serverStatistics.decClientCount();
        --clientCount;
    }
}
