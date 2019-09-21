package me.vzhilin.bstreamer.server.streaming.base;

import me.vzhilin.bstreamer.server.streaming.file.SourceDescription;

public interface PushSource {
    SourceDescription getDesc();

    void subscribe(PushSourceListener listener);
    void unsubscribe(PushSourceListener listener);
}
