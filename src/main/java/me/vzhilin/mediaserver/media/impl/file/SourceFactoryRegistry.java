package me.vzhilin.mediaserver.media.impl.file;

import me.vzhilin.mediaserver.media.impl.MediaPacketSourceFactory;

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
