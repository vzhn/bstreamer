package me.vzhilin.mediaserver.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.rtsp.RtspEncoder;
import io.netty.util.AttributeKey;
import me.vzhilin.mediaserver.client.conf.ClientConfig;
import me.vzhilin.mediaserver.client.conf.ConnectionSettings;
import me.vzhilin.mediaserver.client.rtsp.NettyRtspChannelHandler;
import org.apache.log4j.BasicConfigurator;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;

public class Client {
    private static final AttributeKey<ConnectionStatistics> STAT = AttributeKey.valueOf("stat");
    private static final AttributeKey<Bootstrap> BOOTSTRAP = AttributeKey.valueOf("bootstrap");
    private static final AttributeKey<URI> URL = AttributeKey.valueOf("url");

    private final static ChannelFutureListener ON_CLOSED = new ClosedListener();
    private final static ChannelFutureListener ON_CONNECTED = new ConnectedListener();

    private final Bootstrap b;
    private final TotalStatistics ss;

    private final ClientConfig conf;

    public static void main(String... argv) throws IOException {
        BasicConfigurator.configure();
        ClientConfig conf = ClientConfig.read(new File(argv[0]));
        Client client = new Client(conf);
        client.start();
    }

    public Client(ClientConfig conf) {
        this.conf = conf;
        Bootstrap bootstrap = new Bootstrap();
        ss = new TotalStatistics();

        EventLoopGroup workerGroup = new EpollEventLoopGroup(4);
        b = bootstrap
            .group(workerGroup)
            .channel(EpollSocketChannel.class)
            .option(ChannelOption.SO_RCVBUF, 131072)
            .handler(new ClientChannelInitializer());
    }

    public void start() {
        System.err.println("pid = " + ManagementFactory.getRuntimeMXBean().getName());
        for (ConnectionSettings conn: conf.getConnections()) {
            URI uri = URI.create(conn.getUrl());
            int n = conn.getN();
            for (int i = 0; i < n; i++) {
                ConnectionStatistics stat = ss.newStat();
                Bootstrap btstrp = b.clone().attr(STAT, stat);
                btstrp.attr(BOOTSTRAP, btstrp);
                btstrp.attr(URL, uri);
                btstrp.connect(uri.getHost(), uri.getPort()).addListener(ON_CONNECTED);
            }
        }

        startReporter(ss);
    }

    private void startReporter(TotalStatistics ss) {
        new ClientReporter(ss).start();
    }

    private final static class ConnectedListener implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future) {
            if (future.isSuccess()) {
                Channel channel = future.channel();
                ConnectionStatistics stat = channel.attr(STAT).get();
                stat.onConnected();
                channel.closeFuture()
                        .addListener((ChannelFutureListener) closeFuture -> stat.onDisconnected())
                        .addListener(ON_CLOSED);
            } else {
                Channel channel = future.channel();
                if (!channel.eventLoop().isShuttingDown()) {
                    URI uri = channel.attr(URL).get();
                    Bootstrap b = channel.attr(BOOTSTRAP).get();
                    b.connect(uri.getHost(), uri.getPort()).addListener(ConnectedListener.this);
                }
            }
        }
    }

    private final static class ClosedListener implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future) {
            Channel channel = future.channel();
            if (!channel.eventLoop().isShuttingDown()) {
                URI uri = channel.attr(URL).get();
                Bootstrap b = channel.attr(BOOTSTRAP).get();
                b.connect(uri.getHost(), uri.getPort()).addListener(ON_CONNECTED);
            }
        }
    }

    private final class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {
        private ClientChannelInitializer() { }

        @Override
        protected void initChannel(SocketChannel ch) {
            URI uri = ch.attr(Client.URL).get();
            ChannelPipeline pipeline = ch.pipeline();
            RtspInterleavedDecoder rtspInterleavedDecoder =
                    new RtspInterleavedDecoder(1024, 1024, 64 * 1024);
            rtspInterleavedDecoder.setCumulator(RtspInterleavedDecoder.COMPOSITE_CUMULATOR);
            pipeline.addLast(rtspInterleavedDecoder);
            pipeline.addLast(new RtspEncoder());
            pipeline.addLast(new HttpObjectAggregator(64 * 1024));
            pipeline.addLast(new NettyRtspChannelHandler(new DefaultConnectionHandler(uri)));
            pipeline.addLast(new SimpleChannelInboundHandler<InterleavedPacket>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, InterleavedPacket msg) {
                    ss.onRead(msg.getPayload().readableBytes());
                    msg.getPayload().release();
                }
            });
        }
    }
}
