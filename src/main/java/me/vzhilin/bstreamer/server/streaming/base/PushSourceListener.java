package me.vzhilin.bstreamer.server.streaming.base;

import me.vzhilin.bstreamer.server.streaming.file.MediaPacket;

public interface PushSourceListener {
    void onNext(MediaPacket packet);
    void onEof();

    void onConnected();
    void onDisconnected();
    void onAttached();
    void onDetached();
}
