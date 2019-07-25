package me.vzhilin.mediaserver.media;

import me.vzhilin.mediaserver.media.impl.file.MediaPacket;
import me.vzhilin.mediaserver.media.impl.file.MediaPacketSourceDescription;

import java.io.Closeable;
import java.util.Iterator;

public interface PullSource extends Iterator<MediaPacket>, Closeable {
    MediaPacketSourceDescription getDesc();

    @Override
    boolean hasNext();

    @Override
    MediaPacket next();
}
