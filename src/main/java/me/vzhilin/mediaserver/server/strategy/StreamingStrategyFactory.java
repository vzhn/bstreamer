package me.vzhilin.mediaserver.server.strategy;

import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.file.MediaPacketSourceDescription;

public interface StreamingStrategyFactory {
    StreamingStrategy getStrategy(PropertyMap sourceConfig);
    MediaPacketSourceDescription describe(PropertyMap sourceConfig);
}
