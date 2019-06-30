package me.vzhilin.mediaserver.media.file;

import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.MediaPacketSource;
import me.vzhilin.mediaserver.media.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.media.MediaPaketSourceConfig;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class FileSourceFactory implements MediaPacketSourceFactory {
    @Override
    public MediaPacketSource newSource(PropertyMap sourceProperties) {
        try {
            return new FileMediaPacketSource(sourceProperties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
