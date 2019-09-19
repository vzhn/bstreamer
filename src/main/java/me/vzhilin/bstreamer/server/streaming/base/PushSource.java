package me.vzhilin.bstreamer.server.streaming.base;

import me.vzhilin.bstreamer.server.streaming.file.MediaPacketSourceDescription;

public interface PushSource {
    MediaPacketSourceDescription getDesc();

    void subscribe(PushSourceListener listener);
    void unsubscribe(PushSourceListener listener);
}
