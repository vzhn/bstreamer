package me.vzhilin.mediaserver.server.strategy;

import java.util.HashMap;
import java.util.Map;

public class StreamingStrategyFactoryRegister {
    private final Map<String, StreamingStrategyFactory> strategyFactoryMap = new HashMap<>();

    public void addFactory(String name, StreamingStrategyFactory strategyFactory) {
        strategyFactoryMap.put(name, strategyFactory);
    }
}
