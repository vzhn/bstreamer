package me.vzhilin.mediaserver.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import me.vzhilin.mediaserver.conf.Config;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactoryRegistry;
import me.vzhilin.mediaserver.server.strategy.seq.SequencedStrategyFactory;
import me.vzhilin.mediaserver.server.strategy.sync.SyncStrategyFactory;

public class RtspServer {
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final ServerBootstrap bootstrap;
    private final StreamingStrategyFactoryRegistry streamingStrategyRegistry;
    private final ServerStatistics stat;
    private final Config config;
    private ChannelFuture future;

    public RtspServer(Config config) {
        this.config = config;
        bootstrap = new ServerBootstrap();
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(1);
        stat = new ServerStatistics();
        streamingStrategyRegistry = new StreamingStrategyFactoryRegistry();
        streamingStrategyRegistry.addFactory("sync", new SyncStrategyFactory(workerGroup, stat, config));
        streamingStrategyRegistry.addFactory("seq", new SequencedStrategyFactory());
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
                .attr(RtspServerAttributes.CONFIG, config)
                .attr(RtspServerAttributes.STAT, stat)
                .childHandler(new RtspServerInitializer(streamingStrategyRegistry))
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, writeBufferWaterMark)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        if (config.getNetworkSndbuf().isPresent()) {
            bootstrap.childOption(ChannelOption.SO_SNDBUF, config.getNetworkSndbuf().get());
        }

        future = bootstrap.bind(5000).syncUninterruptibly();
    }

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        future.channel().close().syncUninterruptibly();
    }
}
