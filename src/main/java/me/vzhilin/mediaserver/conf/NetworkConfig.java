package me.vzhilin.mediaserver.conf;

import io.netty.channel.WriteBufferWaterMark;

public class NetworkConfig {
    private final PropertyMap properties;

    public NetworkConfig(PropertyMap properties) {
        this.properties = properties;
    }

    public int sndbuf() {
        return properties.getInt("sndbuf");
    }

    public int port() {
        return properties.getInt("port");
    }

    public WriteBufferWaterMark networkWatermarks() {
        int lowWatermark = properties.getInt("watermarks.low");
        int highWatermark = properties.getInt("watermarks.high");
        return new WriteBufferWaterMark(lowWatermark, highWatermark);
    }
}
