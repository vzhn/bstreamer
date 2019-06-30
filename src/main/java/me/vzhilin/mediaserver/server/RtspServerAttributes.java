package me.vzhilin.mediaserver.server;

import io.netty.util.AttributeKey;

/**
 * RtspServerAttributes
 */
public class RtspServerAttributes {
    /** Context */
    public static final AttributeKey<ServerContext> CONTEXT = AttributeKey.valueOf("context");
}
