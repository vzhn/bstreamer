package me.vzhilin.mediaserver.client.rtsp;

public interface RtspConnectionHandler {
    void onConnected(RtspConnection connection);
    void onDisconnected();
}
