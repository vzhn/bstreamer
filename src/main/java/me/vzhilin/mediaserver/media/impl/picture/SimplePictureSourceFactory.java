package me.vzhilin.mediaserver.media.impl.picture;

import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.PullSource;
import me.vzhilin.mediaserver.media.impl.PullSourceFactory;
import me.vzhilin.mediaserver.server.ServerContext;

public final class SimplePictureSourceFactory implements PullSourceFactory {
    @Override
    public PullSource newSource(ServerContext context, PropertyMap properties) {
        return new SimplePictureSource(context, properties);
    }
}
