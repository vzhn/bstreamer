package me.vzhilin.mediaserver.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import me.vzhilin.mediaserver.conf.Config;
import me.vzhilin.mediaserver.conf.NetworkAttributes;
import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.file.FileSourceFactory;
import me.vzhilin.mediaserver.media.picture.PictureSourceFactory;
import me.vzhilin.mediaserver.server.strategy.sync.SyncStrategyFactory;

public class RtspServer {
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final ServerBootstrap bootstrap;
    private final Config config;
    private final ServerContext serverContext;
    private ChannelFuture future;

    public RtspServer(Config config) {
        this.config = config;
        bootstrap = new ServerBootstrap();
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(1);
        this.serverContext = new ServerContext(config);
        this.serverContext.registerSourceFactory("picture", new PictureSourceFactory());
        this.serverContext.registerSourceFactory("file", new FileSourceFactory());
        this.serverContext.registerSyncStrategy("sync", new SyncStrategyFactory(serverContext));
        this.serverContext.setScheduledExecutor(workerGroup);
    }

    public void start() {
        startMetrics();
        startServer();
    }

    private void startMetrics() {

    }

    private void startServer() {
        PropertyMap network = config.getNetwork();
        int port = network.getInt(NetworkAttributes.PORT);
        int sndbuf = network.getInt(NetworkAttributes.SNDBUF);
        int lowWatermark = network.getInt(NetworkAttributes.WATERMARKS_LOW);
        int highWatermark = network.getInt(NetworkAttributes.WATERMARKS_HIGH);
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childAttr(RtspServerAttributes.CONTEXT, serverContext)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        if (sndbuf > 0) {
            bootstrap.childOption(ChannelOption.SO_SNDBUF, sndbuf);
        }
        if (lowWatermark > 0 && highWatermark > 0) {
            WriteBufferWaterMark writeBufferWaterMark = new WriteBufferWaterMark(lowWatermark, highWatermark);
            bootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, writeBufferWaterMark);
        }
        bootstrap.childHandler(new RtspServerInitializer());
        future = bootstrap.bind(port).syncUninterruptibly();
    }

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        future.channel().close().syncUninterruptibly();
    }
}
