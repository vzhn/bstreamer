package me.vzhilin.mediaserver.server.scheduler;

public interface PushTaskSubscriber {
    void onNext(PushedPacket pp);
    void onEnd();
}
