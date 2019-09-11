package me.vzhilin.bstreamer.server.scheduler;

public interface PushTaskSubscriber {
    void onNext(PushedPacket pp);
    void onEnd();
}
