package me.vzhilin.mediaserver.server.strategy;

public interface StreamingStrategyFactory {
    StreamingStrategy getStrategy(String fileName);
}
