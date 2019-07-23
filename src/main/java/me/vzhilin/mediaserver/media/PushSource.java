package me.vzhilin.mediaserver.media;

public interface PushSource {
    void subscribe(PushSourceListener listener);
    void unsubscribe(PushSourceListener listener);
}
