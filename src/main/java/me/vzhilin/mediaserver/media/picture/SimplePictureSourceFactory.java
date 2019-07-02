package me.vzhilin.mediaserver.media.picture;

import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.MediaPacketSource;
import me.vzhilin.mediaserver.media.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.media.file.BufferedMediaPacketSource;
import me.vzhilin.mediaserver.server.ServerContext;

public class SimplePictureSourceFactory implements MediaPacketSourceFactory {
    private final ServerContext context;

    public SimplePictureSourceFactory(ServerContext context) {
        this.context = context;
    }

    @Override
    public MediaPacketSource newSource(PropertyMap properties) {
        return new BufferedMediaPacketSource(new SimplePictureSource(context, properties), 5);
    }
}
