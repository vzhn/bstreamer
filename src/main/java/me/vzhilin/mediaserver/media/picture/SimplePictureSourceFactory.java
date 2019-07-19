package me.vzhilin.mediaserver.media.picture;

import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.MediaPacketSource;
import me.vzhilin.mediaserver.media.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.media.file.BufferedMediaPacketSource;
import me.vzhilin.mediaserver.server.ServerContext;

public final class SimplePictureSourceFactory implements MediaPacketSourceFactory {
    @Override
    public MediaPacketSource newSource(ServerContext context, PropertyMap properties) {
        return new SimplePictureSource(context, properties);
    }
}
