package me.vzhilin.mediaserver.media;

import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.server.ServerContext;

public interface MediaPacketSourceFactory {
    MediaPacketSource newSource(ServerContext context, PropertyMap sourceConfig);
}
