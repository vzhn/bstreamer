package me.vzhilin.mediaserver.util.scheduler;

import me.vzhilin.mediaserver.InterleavedFrame;

public final class PushedPacket {
    private final InterleavedFrame packet;
    private final Runnable pushNext;
    private boolean drained;

    PushedPacket(Runnable pushNext, InterleavedFrame packet) {
        this.pushNext = pushNext;
        this.packet = packet;
    }

    public InterleavedFrame drain() {
        if (!drained) {
            drained = true;
            pushNext.run();
        }
        return packet;
    }
}
