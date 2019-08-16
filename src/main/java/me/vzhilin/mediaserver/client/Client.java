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
import me.vzhilin.mediaserver.util.HumanReadable;
import org.apache.log4j.BasicConfigurator;

import java.io.File;
import java.io.IOException;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client {
    private static final AttributeKey<ConnectionStatistics> STAT = AttributeKey.valueOf("stat");
    private static final AttributeKey<Bootstrap> BOOTSTRAP = AttributeKey.valueOf("bootstrap");
    private static final AttributeKey<URI> URL = AttributeKey.valueOf("url");

    private final Bootstrap b;
    private final TotalStatistics ss;

    private final static ChannelFutureListener ON_CONNECTED = future -> {
        ConnectionStatistics stat = future.channel().attr(STAT).get();
        stat.onConnected();
    };

    private final static ChannelFutureListener ON_CLOSED = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) {
            Channel channel = future.channel();
            ConnectionStatistics stat = channel.attr(STAT).get();
            URI uri = channel.attr(URL).get();
            Bootstrap b = channel.attr(BOOTSTRAP).get();
            stat.onDisconnected();

            if (!channel.eventLoop().isShuttingDown()) {
                ChannelFuture connectFuture = b.connect(uri.getHost(), uri.getPort());
                connectFuture.addListener(ON_CONNECTED);
                connectFuture.channel().closeFuture().addListener(this);
            }
        }
    };
    private final ClientConfig conf;

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

    public static void main(String... argv) throws IOException {
        BasicConfigurator.configure();
        ClientConfig conf = ClientConfig.read(new File(argv[0]));
        Client client = new Client(conf);
        client.start();
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
                ChannelFuture connectFuture = btstrp.connect(uri.getHost(), uri.getPort());
                bindListener(connectFuture);
            }
        }

        startReporter(ss);
    }

    private void startReporter(TotalStatistics ss) {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable() {
            private TotalStatistics.Snapshot prev;

            @Override
            public void run() {
                TotalStatistics.Snapshot s = ss.snapshot();
                if (prev != null) {
                    List<BufferPoolMXBean> pools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
                    String cap = "";
                    for (BufferPoolMXBean pool : pools) {
                        if ("mapped".equals(pool.getName())) {
                            continue;
                        }
                        long directMemoryUsed = pool.getMemoryUsed();
                        cap = HumanReadable.humanReadableByteCount(directMemoryUsed, false);
                        break;
                    }
                    System.err.print("\r" + ss.getSize() + " " + s.diff(prev) + " " + cap);
                }
                prev = s;
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void bindListener(ChannelFuture connectFuture) {
        connectFuture.addListener(ON_CONNECTED);
        connectFuture.channel().closeFuture().addListener(ON_CLOSED);
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
            pipeline.addLast(new HttpObjectAggregator(104857));
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
