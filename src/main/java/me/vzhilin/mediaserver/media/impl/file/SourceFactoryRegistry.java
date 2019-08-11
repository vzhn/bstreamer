package me.vzhilin.mediaserver.media.impl.file;

import me.vzhilin.mediaserver.media.impl.PullSourceFactory;

import java.util.HashMap;
import java.util.Map;

public class SourceFactoryRegistry {
    private final Map<String, PullSourceFactory> map = new HashMap<>();
    public void register(String name, PullSourceFactory factory) {
        map.put(name, factory);
    }

    public PullSourceFactory get(String name) {
        return map.get(name);
    }
}
