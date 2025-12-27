package me.vzhilin.bstreamer.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import me.vzhilin.bstreamer.server.conf.Config;
import me.vzhilin.bstreamer.server.conf.NetworkAttributes;
import me.vzhilin.bstreamer.util.AppRuntime;
import me.vzhilin.bstreamer.util.PropertyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_ERROR;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;

public class RtspServer {
    private final static Logger LOG = LoggerFactory.getLogger(RtspServer.class);

    private final Class<? extends ServerSocketChannel> channelClazz;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final ServerBootstrap serverBootstrap;
    private final Config serverConfig;
    private final ServerContext serverContext;
    private List<ChannelFuture> bindFutures;

    public RtspServer(Config serverConfig) {
        this.serverConfig = serverConfig;
        this.serverBootstrap = new ServerBootstrap();

        int nThreads = serverConfig.getNetwork().getInt("threads");
        final IoHandlerFactory factory;
        if (AppRuntime.IS_LINUX && Epoll.isAvailable()) {
            factory = EpollIoHandler.newFactory();
            channelClazz = EpollServerSocketChannel.class;
            LOG.info("choosing epoll for i/o");
        } else if (AppRuntime.IS_WINDOWS) {
            factory = NioIoHandler.newFactory();
            channelClazz = NioServerSocketChannel.class;
            LOG.info("choosing NIO for i/o");
        } else if (AppRuntime.IS_MAC && KQueue.isAvailable()) {
            factory = KQueueIoHandler.newFactory();
            channelClazz = KQueueServerSocketChannel.class;
            LOG.info("choosing KQueue for i/o");
        } else {
            factory = NioIoHandler.newFactory();
            channelClazz = NioServerSocketChannel.class;
            LOG.info("fallback: choosing NIO for i/o");
        }

        bossGroup = new MultiThreadIoEventLoopGroup(1, factory);
        workerGroup = new MultiThreadIoEventLoopGroup(nThreads, factory);

        this.serverContext = new ServerContext(serverConfig);
    }

    public ServerContext getServerContext() {
        return serverContext;
    }

    public void start() {
        setupLoglevel();
        startServer();
    }

    private void setupLoglevel() {
        av_log_set_level(AV_LOG_ERROR);
    }

    private void startServer() {
        PropertyMap network = serverConfig.getNetwork();
        int sndbuf = network.getInt(NetworkAttributes.SNDBUF);
        int lowWatermark = network.getInt(NetworkAttributes.WATERMARKS_LOW);
        int highWatermark = network.getInt(NetworkAttributes.WATERMARKS_HIGH);
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(channelClazz)
                .childAttr(RtspServerAttributes.CONTEXT, serverContext)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BACKLOG, 20000)
                .childOption(ChannelOption.SO_LINGER, 0)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        if (sndbuf > 0) {
            serverBootstrap.childOption(ChannelOption.SO_SNDBUF, sndbuf);
        }
        if (lowWatermark > 0 && highWatermark > 0) {
            WriteBufferWaterMark writeBufferWaterMark = new WriteBufferWaterMark(lowWatermark, highWatermark);
            serverBootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, writeBufferWaterMark);
        }
        serverBootstrap.childHandler(new RtspServerInitializer());
        Collection<SocketAddress> addresses = toSocketAddress(network.getStringArray(NetworkAttributes.BIND));

        bindFutures = addresses.stream()
                .map(sa -> serverBootstrap.bind(sa).syncUninterruptibly())
                .collect(Collectors.toList());

    }

    private Collection<SocketAddress> toSocketAddress(List<String> hostAndPort) {
        return hostAndPort.stream().map((Function<String, SocketAddress>) s -> {
            int colonPos = s.indexOf(':');
            String host = s.substring(0, colonPos);
            int port = Integer.parseInt(s.substring(colonPos+1));
            return new InetSocketAddress(host, port);
        }).collect(Collectors.toList());
    }

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        bindFutures.forEach(f -> f.channel().close().syncUninterruptibly());
    }
}
