package me.vzhilin.mediaserver.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import me.vzhilin.mediaserver.conf.Config;
import me.vzhilin.mediaserver.conf.NetworkAttributes;
import me.vzhilin.mediaserver.conf.PropertyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

import static org.bytedeco.javacpp.avutil.AV_LOG_ERROR;
import static org.bytedeco.javacpp.avutil.av_log_set_level;

public class RtspServer {
    private final static Logger LOG = LoggerFactory.getLogger(RtspServer.class);

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final ServerBootstrap bootstrap;
    private final Config config;
    private final ServerContext serverContext;
    private ChannelFuture future;

    public RtspServer(Config config) {
        this.config = config;
        this.bootstrap = new ServerBootstrap();
        this.bossGroup = new EpollEventLoopGroup(1);
        this.workerGroup = new EpollEventLoopGroup(1);
        this.serverContext = new ServerContext(config);
        this.serverContext.setScheduledExecutor(Executors.newSingleThreadScheduledExecutor());
    }

    public ServerContext getServerContext() {
        return serverContext;
    }

    public void start() {
        setupLoglevel();
        startMetrics();
        startServer();

        LOG.info("mediaserver started");
    }

    private void setupLoglevel() {
        av_log_set_level(AV_LOG_ERROR);
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
                .channel(EpollServerSocketChannel.class)
                .childAttr(RtspServerAttributes.CONTEXT, serverContext)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BACKLOG, 20000)
                .childOption(ChannelOption.SO_LINGER, 0)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        if (sndbuf > 0) {
            bootstrap.childOption(ChannelOption.SO_SNDBUF, sndbuf);
        }
        if (lowWatermark > 0 && highWatermark > 0) {
            WriteBufferWaterMark writeBufferWaterMark = new WriteBufferWaterMark(lowWatermark, highWatermark);
            bootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, writeBufferWaterMark);
        }
        bootstrap.childHandler(new RtspServerInitializer());
        future = bootstrap.bind("0.0.0.0", port).syncUninterruptibly();
    }

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        future.channel().close().syncUninterruptibly();
    }
}
