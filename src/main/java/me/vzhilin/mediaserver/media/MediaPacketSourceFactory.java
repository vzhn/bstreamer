package me.vzhilin.mediaserver.media;

public interface MediaPacketSourceFactory {
    MediaPacketSource newSource(MediaPaketSourceConfig sourceConfig);
}
