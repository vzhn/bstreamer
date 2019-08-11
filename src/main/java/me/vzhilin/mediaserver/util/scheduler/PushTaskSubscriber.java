package me.vzhilin.mediaserver.util.scheduler;

public interface PushTaskSubscriber {
    void onNext(PushedPacket pp);
    void onEnd();
}
