package me.vzhilin.mediaserver.server;

import io.netty.util.AttributeKey;
import me.vzhilin.mediaserver.conf.Config;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;

/**
 * RtspServerAttributes
 */
public class RtspServerAttributes {
    /** statistics */
    public static final AttributeKey<ServerStatistics> STAT = AttributeKey.valueOf("stat");

    /** config */
    public static final AttributeKey<Config> CONFIG = AttributeKey.valueOf("config");
}
