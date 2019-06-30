package me.vzhilin.mediaserver.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import me.vzhilin.mediaserver.conf.Config;
import me.vzhilin.mediaserver.media.file.FileSourceFactory;
import me.vzhilin.mediaserver.media.file.SourceFactoryRegistry;
import me.vzhilin.mediaserver.media.picture.PictureSourceFactory;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactoryRegistry;
import me.vzhilin.mediaserver.server.strategy.sync.SyncStrategyFactory;

public class RtspServer {
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final ServerBootstrap bootstrap;
    private final StreamingStrategyFactoryRegistry streamingStrategyRegistry;
    private final SourceFactoryRegistry sourceFactoryRegistry;
    private final ServerStatistics stat;
    private final Config config;
    private ChannelFuture future;

    public RtspServer(Config config) {
        this.config = config;
        bootstrap = new ServerBootstrap();
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(1);
        stat = new ServerStatistics();
        sourceFactoryRegistry = new SourceFactoryRegistry();
        sourceFactoryRegistry.register("picture", new PictureSourceFactory());
        sourceFactoryRegistry.register("file", new FileSourceFactory());
        streamingStrategyRegistry = new StreamingStrategyFactoryRegistry();
        streamingStrategyRegistry.addFactory("sync", new SyncStrategyFactory(workerGroup, stat, config, sourceFactoryRegistry));
    }

    public void start() {
        startMetrics();
        startServer();
    }

    private void startMetrics() {

    }

    private void startServer() {
        WriteBufferWaterMark writeBufferWaterMark = config.getNetworkWatermarks();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childAttr(RtspServerAttributes.CONFIG, config)
                .childAttr(RtspServerAttributes.STAT, stat)
                .childHandler(new RtspServerInitializer(streamingStrategyRegistry, sourceFactoryRegistry))
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, writeBufferWaterMark)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        bootstrap.childOption(ChannelOption.SO_SNDBUF, config.getNetworkSndbuf());
        future = bootstrap.bind(config.getPort()).syncUninterruptibly();
    }

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        future.channel().close().syncUninterruptibly();
    }
}
