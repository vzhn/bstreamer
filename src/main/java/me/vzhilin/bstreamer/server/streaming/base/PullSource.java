package me.vzhilin.bstreamer.server.streaming.base;

import me.vzhilin.bstreamer.server.streaming.file.MediaPacket;
import me.vzhilin.bstreamer.server.streaming.file.SourceDescription;

import java.io.Closeable;
import java.util.Iterator;

public interface PullSource extends Iterator<MediaPacket>, Closeable {
    SourceDescription getDesc();

    @Override
    boolean hasNext();

    @Override
    MediaPacket next();
}
