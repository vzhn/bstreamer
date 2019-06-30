package me.vzhilin.mediaserver.server.strategy;

import me.vzhilin.mediaserver.media.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.media.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.media.MediaPaketSourceConfig;

public interface StreamingStrategyFactory {
    StreamingStrategy getStrategy(MediaPaketSourceConfig sourceConfig);
    MediaPacketSourceDescription describe(MediaPaketSourceConfig sourceConfig);
}
