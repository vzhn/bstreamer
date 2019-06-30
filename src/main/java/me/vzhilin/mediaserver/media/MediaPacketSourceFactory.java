package me.vzhilin.mediaserver.media;

import me.vzhilin.mediaserver.conf.PropertyMap;

public interface MediaPacketSourceFactory {
    MediaPacketSource newSource(PropertyMap sourceConfig);
}
