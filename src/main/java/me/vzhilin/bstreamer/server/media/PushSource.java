package me.vzhilin.bstreamer.server.media;

import me.vzhilin.bstreamer.server.media.impl.file.MediaPacketSourceDescription;

public interface PushSource {
    MediaPacketSourceDescription getDesc();

    void subscribe(PushSourceListener listener);
    void unsubscribe(PushSourceListener listener);
}
