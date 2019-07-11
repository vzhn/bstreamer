package me.vzhilin.mediaserver.media.file;

import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.MediaPacketSource;
import me.vzhilin.mediaserver.media.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.server.ServerContext;

import java.io.IOException;

public class FileSourceFactory implements MediaPacketSourceFactory {
    @Override
    public MediaPacketSource newSource(ServerContext c, PropertyMap sourceProperties) {
        try {
            return new FileMediaPacketSource(sourceProperties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
