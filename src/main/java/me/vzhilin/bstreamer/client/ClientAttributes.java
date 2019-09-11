package me.vzhilin.bstreamer.client;

import io.netty.util.AttributeKey;

import java.net.URI;

public final class ClientAttributes {
    public static final AttributeKey<ConnectionStatistics> STAT = AttributeKey.valueOf("stat");
    public static final AttributeKey<URI> URL = AttributeKey.valueOf("url");

    private ClientAttributes() { }
}
