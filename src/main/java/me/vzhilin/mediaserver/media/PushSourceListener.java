package me.vzhilin.mediaserver.media;

import me.vzhilin.mediaserver.media.impl.file.MediaPacket;

public interface PushSourceListener {
    void onNext(MediaPacket packet);
    void onConnected();
    void onDisconnected();
    void onAttached();
    void onDetached();
}
