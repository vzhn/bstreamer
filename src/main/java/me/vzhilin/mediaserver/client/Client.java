package me.vzhilin.mediaserver.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.util.AttributeKey;
import org.apache.log4j.BasicConfigurator;

import javax.management.MXBean;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Client {
    public static final AttributeKey<ConnectionStatistics> STAT = AttributeKey.valueOf("stat");
    private static final String inetHost = "127.0.0.1";

    public static void main(String... argv) {
        Client client = new Client();

        client.start();
    }

    public void start() {
        AtomicLong counter = new AtomicLong(0);

        System.err.println("pid = " + ManagementFactory.getRuntimeMXBean().getName());

        BasicConfigurator.configure();
        Bootstrap bootstrap = new Bootstrap();

        EventLoopGroup workerGroup = new EpollEventLoopGroup(1);
        Bootstrap b = bootstrap
            .group(workerGroup)
            .channel(EpollSocketChannel.class)
            .option(ChannelOption.SO_RCVBUF, 512 * 1024)
            .attr(AttributeKey.<String>valueOf("url"), "rtsp://localhost:5000/file/simpsons_video.mkv")
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    RtspInterleavedDecoder rtspInterleavedDecoder = new RtspInterleavedDecoder(1024, 1024, 64 * 1024);
                    rtspInterleavedDecoder.setCumulator(RtspInterleavedDecoder.COMPOSITE_CUMULATOR);
                    pipeline.addLast(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            counter.addAndGet(((ByteBuf) msg).readableBytes());
                            super.channelRead(ctx, msg);
                        }
                    });
                    pipeline.addLast(rtspInterleavedDecoder);
                    pipeline.addLast(new HttpClientCodec());
                    pipeline.addLast(new ClientHandler());
                }
            });

        TotalStatistics ss = new TotalStatistics();
        ss.onStart();

        for (int i = 0; i < 10 * 2000; i++) {
            ConnectionStatistics stat = ss.newStat();
            Bootstrap btstrp = b.clone();
            btstrp.attr(STAT, stat);
            ChannelFuture future = btstrp.connect(inetHost, 5000);
            bindListener(btstrp, future);
        }

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable() {
            private TotalStatistics.Snapshot prev;

            @Override
            public void run() {
                TotalStatistics.Snapshot s = ss.snapshot();
                if (prev != null) {

                    System.err.println(counter.get() + " " + s.diff(prev) + " " + Runtime.getRuntime().freeMemory());
                }
                prev = s;
            }
        }, 1, 1, TimeUnit.SECONDS);
//        exec.schedule(
//            new Runnable() {
//                @Override
//                public void run() {
//                    ss.onShutdown();
//                    System.err.println(ss);
//                    System.err.println(ss.getSize());
//
//                    workerGroup.shutdownGracefully().syncUninterruptibly();
//                    exec.shutdownNow();
//                }
//            }
//        , 500 * 1000, TimeUnit.SECONDS);
    }

    private void bindListener(Bootstrap b, ChannelFuture connectFuture) {
        ChannelFutureListener connectListener = new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) {
                ConnectionStatistics stat = future.channel().attr(STAT).get();
                stat.onConnected();
            }
        };
        connectFuture.addListener(connectListener);

        ChannelFutureListener closeListener = new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                ConnectionStatistics stat = future.channel().attr(STAT).get();
                stat.onDisconnected();

                if (!future.channel().eventLoop().isShuttingDown()) {
                    ChannelFuture connectFuture = b.connect(inetHost, 5000);
                    connectFuture.addListener(connectListener);
                    connectFuture.channel().closeFuture().addListener(this);
                }
            }
        };
        connectFuture.channel().closeFuture().addListener(closeListener);
    }
}
