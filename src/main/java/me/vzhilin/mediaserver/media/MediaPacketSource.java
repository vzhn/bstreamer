package me.vzhilin.mediaserver.media;

import java.io.Closeable;

public interface MediaPacketSource extends Closeable {
    MediaPacketSourceDescription getDesc();

    boolean hasNext();
    MediaPacket next();
}
