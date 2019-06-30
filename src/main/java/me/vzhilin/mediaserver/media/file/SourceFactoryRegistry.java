package me.vzhilin.mediaserver.media.file;

import java.util.HashMap;
import java.util.Map;

public class SourceFactoryRegistry {
    private final Map<String, MediaPacketSourceFactory> map = new HashMap<>();
    public void register(String name, MediaPacketSourceFactory factory) {
        map.put(name, factory);
    }

    public MediaPacketSourceFactory get(String name) {
        return map.get(name);
    }
}
