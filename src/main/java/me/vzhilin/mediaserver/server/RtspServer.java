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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.bytedeco.javacpp.avutil.AV_LOG_ERROR;
import static org.bytedeco.javacpp.avutil.av_log_set_level;

public class RtspServer {
    private final static Logger LOG = LoggerFactory.getLogger(RtspServer.class);

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final ServerBootstrap serverBootstrap;
    private final Config serverConfig;
    private final ServerContext serverContext;
    private List<ChannelFuture> bindFutures;

    public RtspServer(Config serverConfig) {
        this.serverConfig = serverConfig;
        this.serverBootstrap = new ServerBootstrap();
        this.bossGroup = new EpollEventLoopGroup(1);
        this.workerGroup = new EpollEventLoopGroup(serverConfig.getNetwork().getInt("threads"));
        this.serverContext = new ServerContext(serverConfig);
    }

    public ServerContext getServerContext() {
        return serverContext;
    }

    public void start() {
        setupLoglevel();
        startServer();

        LOG.info("mediaserver started");
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
                .channel(EpollServerSocketChannel.class)
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
