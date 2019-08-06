package me.vzhilin.mediaserver.server.stat;

import me.vzhilin.mediaserver.conf.PropertyMap;

import java.util.HashMap;
import java.util.Map;

public final class ServerStatistics {
    private final Map<PropertyMap, GroupStatistics> groupStats = new HashMap<>();
    private final GroupStatistics totalStats = new GroupStatistics();

    public ServerStatistics() { }

    public void openConn(PropertyMap key) {
        get(key).incOpenConn();
        totalStats.incOpenConn();
    }

    public void closeConn(PropertyMap key) {
        get(key).incCloseConn();
        totalStats.incCloseConn();
    }

    public void incByteCount(PropertyMap key, int bytes) {
        get(key).incByteCount(bytes);
        totalStats.incByteCount(bytes);
    }

    public void incLateCount(PropertyMap key) {
        get(key).incLateCount();
        totalStats.incLateCount();
    }

    public GroupStatistics get(PropertyMap key) {
        GroupStatistics gs;
        if (!groupStats.containsKey(key)) {
            gs = new GroupStatistics();
            groupStats.put(key, gs);
        } else {
            gs = groupStats.get(key);
        }
        return gs;
    }

    public GroupStatistics getTotal() {
        return totalStats;
    }
}
