package me.vzhilin.mediaserver.server;

import io.netty.util.AttributeKey;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;

public class RtspServerAttributes {
    public static final AttributeKey<ServerStatistics> STAT = AttributeKey.valueOf("stat");
}
