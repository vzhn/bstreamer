package me.vzhilin.mediaserver.stream.impl;

import me.vzhilin.mediaserver.InterleavedFrame;
import me.vzhilin.mediaserver.stream.ItemFactory;

import java.util.Iterator;
import java.util.List;

public class LinearBuffer implements ItemFactory<InterleavedFrame> {
    private final Iterator<InterleavedFrame> it;

    public LinearBuffer(List<InterleavedFrame> packets) {
        it = packets.iterator();
    }

    @Override
    public InterleavedFrame next() {
        if (!it.hasNext()) {
            return null;
        }

        InterleavedFrame packet = it.next();
        packet.getPayload().retain();
        return packet;
    }

    @Override
    public void free(InterleavedFrame item) {
        item.getPayload().release();
    }
}
