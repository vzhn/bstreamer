package me.vzhilin.mediaserver.media.picture;

import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.MediaPacketSource;
import me.vzhilin.mediaserver.media.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.media.file.BufferedMediaPacketSource;

public class PictureSourceFactory implements MediaPacketSourceFactory {
    @Override
    public MediaPacketSource newSource(PropertyMap properties) {
        return new BufferedMediaPacketSource(new PictureSource(properties), 20);
    }
}
