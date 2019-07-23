package me.vzhilin.mediaserver.media.impl.file;

import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.PullSource;
import me.vzhilin.mediaserver.media.impl.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.server.ServerContext;

import java.io.IOException;

public class FileSourceFactory implements MediaPacketSourceFactory {
    @Override
    public PullSource newSource(ServerContext c, PropertyMap sourceProperties) {
        try {
            return new FileMediaPacketSource(sourceProperties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
