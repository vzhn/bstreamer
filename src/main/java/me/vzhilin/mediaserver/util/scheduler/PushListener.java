package me.vzhilin.mediaserver.util.scheduler;

public interface PushListener {
    void next(PushedPacket scheduledMediaPacket);
    void end();
}
