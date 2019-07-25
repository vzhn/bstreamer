package me.vzhilin.mediaserver.media;

import me.vzhilin.mediaserver.media.impl.file.MediaPacketSourceDescription;

public interface PushSource {
    MediaPacketSourceDescription getDesc();

    void subscribe(PushSourceListener listener);
    void unsubscribe(PushSourceListener listener);
}
