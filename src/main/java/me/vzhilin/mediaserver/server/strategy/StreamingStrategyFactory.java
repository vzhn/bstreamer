package me.vzhilin.mediaserver.server.strategy;

import me.vzhilin.mediaserver.media.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.media.MediaPacketSourceFactory;

public interface StreamingStrategyFactory {
    StreamingStrategy getStrategy(MediaPacketSourceFactory sourceFactory);
    MediaPacketSourceDescription describe(MediaPacketSourceFactory sourceFactory);
}
