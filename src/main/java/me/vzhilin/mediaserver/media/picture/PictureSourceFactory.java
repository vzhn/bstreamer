package me.vzhilin.mediaserver.media.picture;

import me.vzhilin.mediaserver.media.file.BufferedMediaPacketSource;
import me.vzhilin.mediaserver.media.file.MediaPacketSource;
import me.vzhilin.mediaserver.media.file.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.media.file.MediaPaketSourceConfig;

public class PictureSourceFactory implements MediaPacketSourceFactory {
    private final H264CodecParameters h264CodecParameters;

    public PictureSourceFactory(H264CodecParameters h264CodecParameters) {
        this.h264CodecParameters = h264CodecParameters;
    }

    @Override
    public MediaPacketSource newSource(MediaPaketSourceConfig config) {
        return new BufferedMediaPacketSource(new PictureSource(h264CodecParameters), 20);
    }
}
