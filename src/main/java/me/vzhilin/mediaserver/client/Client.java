package me.vzhilin.mediaserver.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.rtsp.RtspEncoder;
import io.netty.util.AttributeKey;
import me.vzhilin.mediaserver.client.rtsp.NettyRtspChannelHandler;
import me.vzhilin.mediaserver.client.rtsp.RtspConnectionHandler;
import me.vzhilin.mediaserver.util.HumanReadable;
import org.apache.log4j.BasicConfigurator;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client {
    public static final AttributeKey<ConnectionStatistics> STAT = AttributeKey.valueOf("stat");
    public static final AttributeKey<Bootstrap> BOOTSTRAP = AttributeKey.valueOf("bootstrap");
    private static final String inetHost = "localhost";
    public static final int INET_PORT = 5000;
    private final Bootstrap b;
    private final TotalStatistics ss;

    private final ChannelFutureListener onConnected = future -> {
        ConnectionStatistics stat = future.channel().attr(STAT).get();
        stat.onConnected();
    };

    private final ChannelFutureListener onClosed = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) {
            Channel channel = future.channel();
            ConnectionStatistics stat = channel.attr(STAT).get();
            Bootstrap b = channel.attr(BOOTSTRAP).get();
            stat.onDisconnected();

            if (!channel.eventLoop().isShuttingDown()) {
                ChannelFuture connectFuture = b.connect(inetHost, INET_PORT);
                connectFuture.addListener(onConnected);
                connectFuture.channel().closeFuture().addListener(this);
            }
        }
    };

    public Client(String[] argv) {
        Bootstrap bootstrap = new Bootstrap();
        ss = new TotalStatistics();
        RtspConnectionHandler handler = new DefaultConnectionHandler();

        EventLoopGroup workerGroup = new EpollEventLoopGroup(4);
        String url = "rtsp://localhost:5000/file/simpsons_video.mkv";
        b = bootstrap
            .group(workerGroup)
            .channel(EpollSocketChannel.class)
            .option(ChannelOption.SO_RCVBUF, 131072)
            .attr(AttributeKey.<String>valueOf("url"), url)
            .handler(new ClientChannelInitializer(url, handler));
    }

    public static void main(String... argv) {
        Client client = new Client(argv);
        client.start();
    }

    public void start() {
        System.err.println("pid = " + ManagementFactory.getRuntimeMXBean().getName());

        BasicConfigurator.configure();
        for (int i = 0; i < 4 * 1000; i++) {
            ConnectionStatistics stat = ss.newStat();
            Bootstrap btstrp = b.clone();
            btstrp.attr(STAT, stat);
            btstrp.attr(BOOTSTRAP, btstrp);

            bindListener(btstrp.connect(inetHost, INET_PORT));
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
        connectFuture.addListener(onConnected);
        connectFuture.channel().closeFuture().addListener(onClosed);
    }

    private class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {
        private final String url;
        private final RtspConnectionHandler handler;

        private ClientChannelInitializer(String url, RtspConnectionHandler handler) {
            this.url = url;
            this.handler = handler;
        }

        @Override
        protected void initChannel(SocketChannel ch) {
            ChannelPipeline pipeline = ch.pipeline();
            RtspInterleavedDecoder rtspInterleavedDecoder =
                    new RtspInterleavedDecoder(1024, 1024, 64 * 1024);
            rtspInterleavedDecoder.setCumulator(RtspInterleavedDecoder.COMPOSITE_CUMULATOR);
            pipeline.addLast(rtspInterleavedDecoder);
            pipeline.addLast("http_codec", new RtspEncoder());
            pipeline.addLast(new HttpObjectAggregator(104857));
            pipeline.addLast(new NettyRtspChannelHandler(URI.create(url), handler));
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
