package me.vzhilin.mediaserver.stream.impl;

import me.vzhilin.mediaserver.InterleavedFrame;
import me.vzhilin.mediaserver.stream.ItemFactory;

import java.util.Iterator;
import java.util.List;

public class CircularBuffer implements ItemFactory<InterleavedFrame> {
    private final List<InterleavedFrame> packets;
    private Iterator<InterleavedFrame> it;

    public CircularBuffer(List<InterleavedFrame> packets) {
        this.packets = packets;
        it = packets.iterator();
    }

    @Override
    public InterleavedFrame next() {
        if (!it.hasNext()) {
            it = packets.iterator();
        }

        InterleavedFrame pkt = it.next();
        pkt.getPayload().retain();
        return pkt;
    }

    @Override
    public void free(InterleavedFrame item) {
        item.getPayload().release();
    }
}
