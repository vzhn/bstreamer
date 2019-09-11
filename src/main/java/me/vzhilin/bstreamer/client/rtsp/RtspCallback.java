package me.vzhilin.bstreamer.client.rtsp;

public interface RtspCallback<T> {
    void onSuccess(T mesg);
    void onError();
}
