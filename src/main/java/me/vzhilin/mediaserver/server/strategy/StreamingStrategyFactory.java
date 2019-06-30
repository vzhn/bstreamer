package me.vzhilin.mediaserver.server.strategy;

import me.vzhilin.mediaserver.media.file.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.media.file.MediaPaketSourceConfig;

public interface StreamingStrategyFactory {
    StreamingStrategy getStrategy(MediaPaketSourceConfig sourceConfig);
    MediaPacketSourceDescription describe(MediaPaketSourceConfig sourceConfig);
}
