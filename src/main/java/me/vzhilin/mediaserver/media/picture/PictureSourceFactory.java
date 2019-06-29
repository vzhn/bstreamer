package me.vzhilin.mediaserver.media.picture;

import me.vzhilin.mediaserver.media.BufferedMediaPacketSource;
import me.vzhilin.mediaserver.media.MediaPacketSource;
import me.vzhilin.mediaserver.media.MediaPacketSourceFactory;

public class PictureSourceFactory implements MediaPacketSourceFactory {
    @Override
    public MediaPacketSource newSource() {
        return new BufferedMediaPacketSource(new PictureSource(), 20);
    }
}
