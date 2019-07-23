package me.vzhilin.mediaserver.media.impl.picture;

import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.PullSource;
import me.vzhilin.mediaserver.media.impl.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.server.ServerContext;

public final class SimplePictureSourceFactory implements MediaPacketSourceFactory {
    @Override
    public PullSource newSource(ServerContext context, PropertyMap properties) {
        return new SimplePictureSource(context, properties);
    }
}
