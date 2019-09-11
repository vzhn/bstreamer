package me.vzhilin.bstreamer.server.media;

import me.vzhilin.bstreamer.server.media.impl.file.MediaPacket;

public interface PushSourceListener {
    void onNext(MediaPacket packet);
    void onEof();

    void onConnected();
    void onDisconnected();
    void onAttached();
    void onDetached();
}
