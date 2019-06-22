package me.vzhilin.mediaserver.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactoryRegistry;
import me.vzhilin.mediaserver.server.strategy.seq.SequencedStrategyFactory;
import me.vzhilin.mediaserver.server.strategy.sync.SyncStrategyFactory;

public class RtspServer {
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final ServerBootstrap bootstrap;
    private final StreamingStrategyFactoryRegistry registry;
    private ChannelFuture future;

    public RtspServer() {
        bootstrap = new ServerBootstrap();
        bossGroup = new EpollEventLoopGroup(1);
        workerGroup = new EpollEventLoopGroup(1);

        registry = new StreamingStrategyFactoryRegistry();
        registry.addFactory("sync", new SyncStrategyFactory(workerGroup));
        registry.addFactory("seq", new SequencedStrategyFactory());
    }

    public void start() {
        WriteBufferWaterMark writeBufferWaterMark = new WriteBufferWaterMark(256 * 1024, 512 * 1024);
        bootstrap.group(bossGroup, workerGroup)
                .channel(EpollServerSocketChannel.class)
                .childHandler(new RtspServerInitializer(registry))
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, writeBufferWaterMark)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.SO_SNDBUF, 8 * 1024);

        future = bootstrap.bind(5000).syncUninterruptibly();
    }

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        future.channel().close().syncUninterruptibly();
    }
}
