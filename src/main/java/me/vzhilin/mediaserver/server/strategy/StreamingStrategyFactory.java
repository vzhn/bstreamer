package me.vzhilin.mediaserver.server.strategy;

import me.vzhilin.mediaserver.media.MediaPacketSourceDescription;

public interface StreamingStrategyFactory {
    StreamingStrategy getStrategy(String fileName);
    MediaPacketSourceDescription describe(String fileName);
}
