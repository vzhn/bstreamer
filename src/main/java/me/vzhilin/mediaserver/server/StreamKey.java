package me.vzhilin.mediaserver.server;

import io.netty.channel.EventLoop;
import me.vzhilin.mediaserver.conf.PropertyMap;

import java.util.Objects;

final class StreamKey {
    public final EventLoop eventLoop;
    public final String url;
    public final SourceKey sourceKey;

    StreamKey(EventLoop eventLoop, String url, String clazz, PropertyMap cfg) {
        this.eventLoop = eventLoop;
        this.url = url;
        this.sourceKey = new SourceKey(clazz, cfg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StreamKey streamKey = (StreamKey) o;
        return eventLoop.equals(streamKey.eventLoop) &&
                url.equals(streamKey.url) &&
                sourceKey.equals(streamKey.sourceKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventLoop, url, sourceKey);
    }
}
