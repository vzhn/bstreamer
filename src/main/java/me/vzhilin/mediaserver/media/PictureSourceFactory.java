package me.vzhilin.mediaserver.media;

public class PictureSourceFactory implements MediaPacketSourceFactory {
    @Override
    public MediaPacketSource newSource() {
        return new PictureSource();
    }
}
