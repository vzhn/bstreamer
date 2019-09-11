package me.vzhilin.bstreamer.server.scheduler;

import me.vzhilin.bstreamer.server.media.InterleavedFrame;

public final class PushedPacket {
    private final InterleavedFrame packet;
    private final Runnable pushNext;

    private final int subsCount;
    private int subsProcessed;

    PushedPacket(Runnable pushNext, InterleavedFrame packet, int subsCount) {
        this.pushNext = pushNext;
        this.packet = packet;
        this.subsCount = subsCount;
    }

    public InterleavedFrame drain() {
        boolean ready;
        synchronized (this) {
            ++subsProcessed;
            ready = subsProcessed == subsCount;
        }

        if (ready) {
            pushNext.run();
        }
        return packet;
    }
}
