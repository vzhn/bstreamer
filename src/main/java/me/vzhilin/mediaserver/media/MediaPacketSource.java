package me.vzhilin.mediaserver.media;

import java.io.Closeable;

public interface MediaPacketSource extends Closeable {
    boolean hasNext();
    MediaPacket next();
}
