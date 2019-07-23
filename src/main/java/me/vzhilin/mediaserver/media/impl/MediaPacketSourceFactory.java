package me.vzhilin.mediaserver.media.impl;

import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.PullSource;
import me.vzhilin.mediaserver.server.ServerContext;

public interface MediaPacketSourceFactory {
    PullSource newSource(ServerContext context, PropertyMap sourceConfig);
}
