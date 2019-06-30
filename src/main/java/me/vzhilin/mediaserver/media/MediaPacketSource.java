package me.vzhilin.mediaserver.media;

import me.vzhilin.mediaserver.media.file.MediaPacket;
import me.vzhilin.mediaserver.media.file.MediaPacketSourceDescription;

import java.io.Closeable;
import java.util.Iterator;

public interface MediaPacketSource extends Iterator<MediaPacket>, Closeable {
    /**
     * @return media description
     */
    MediaPacketSourceDescription getDesc();

    @Override
    boolean hasNext();

    @Override
    MediaPacket next();
}
