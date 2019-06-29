package me.vzhilin.mediaserver.media.picture;

import me.vzhilin.mediaserver.media.BufferedMediaPacketSource;
import me.vzhilin.mediaserver.media.MediaPacketSource;
import me.vzhilin.mediaserver.media.MediaPacketSourceFactory;

public class PictureSourceFactory implements MediaPacketSourceFactory {
    private final H264CodecParameters h264CodecParameters;

    public PictureSourceFactory(H264CodecParameters h264CodecParameters) {
        this.h264CodecParameters = h264CodecParameters;
    }

    @Override
    public MediaPacketSource newSource() {
        return new BufferedMediaPacketSource(new PictureSource(h264CodecParameters), 20);
    }
}
