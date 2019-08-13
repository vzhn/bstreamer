package me.vzhilin.mediaserver.client.rtsp;

public interface RtspCallback<T> {
    void onSuccess(T mesg);
    void onError();
}
