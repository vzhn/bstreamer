package me.vzhilin.mediaserver.media;

import java.io.Closeable;
import java.util.Iterator;

public interface MediaPacketSource extends Iterator<MediaPacket>, Closeable {
    MediaPacketSourceDescription getDesc();

    @Override
    boolean hasNext();

    @Override
    MediaPacket next();
}
