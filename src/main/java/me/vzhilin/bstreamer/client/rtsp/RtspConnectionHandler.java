package me.vzhilin.bstreamer.client.rtsp;

public interface RtspConnectionHandler {
    void onConnected(RtspConnection connection);
    void onDisconnected();
}
