package me.vzhilin.mediaserver.media.file;

public interface MediaPacketSourceFactory {
    MediaPacketSource newSource(MediaPaketSourceConfig sourceConfig);
}
