package me.vzhilin.bstreamer.server.streaming.base;

import me.vzhilin.bstreamer.server.streaming.file.MediaPacket;

import java.io.Closeable;
import java.util.Iterator;

public interface PullSource extends Iterator<MediaPacket>, Closeable {
    me.vzhilin.bstreamer.server.streaming.file.MediaPacketSourceDescription getDesc();

    @Override
    boolean hasNext();

    @Override
    MediaPacket next();
}
