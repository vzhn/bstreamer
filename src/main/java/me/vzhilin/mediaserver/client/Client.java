package me.vzhilin.mediaserver.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.apache.log4j.BasicConfigurator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client {
    public static final AttributeKey<ConnectionStatistics> STAT = AttributeKey.valueOf("stat");

    public static void main(String... argv) {
        Client client = new Client();
        client.start();
    }

    public void start() {
        BasicConfigurator.configure();
        Bootstrap bootstrap = new Bootstrap();

        EpollEventLoopGroup workerGroup = new EpollEventLoopGroup();
        Bootstrap b = bootstrap
            .group(workerGroup)
            .channel(EpollSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    RtspInterleavedDecoder rtspInterleavedDecoder = new RtspInterleavedDecoder();
                    rtspInterleavedDecoder.setCumulator(RtspInterleavedDecoder.COMPOSITE_CUMULATOR);

                    pipeline.addLast(rtspInterleavedDecoder);
                    pipeline.addLast(new HttpClientCodec());
                    pipeline.addLast(new ClientHandler());
                }
            });

        TotalStatistics ss = new TotalStatistics();
        ss.onStart();

        for (int i = 0; i < 4 * 1024; i++) {
            ConnectionStatistics stat = ss.newStat();
            Bootstrap btstrp = b.clone();
            btstrp.attr(STAT, stat);
            ChannelFuture future = btstrp.connect("localhost", 5000);
            bindListener(btstrp, future);
        }

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable() {
            private TotalStatistics.Snapshot prev;

            @Override
            public void run() {
                TotalStatistics.Snapshot s = ss.snapshot();
                if (prev != null) {
                    System.err.println(s.diff(prev));
                }
                prev = s;
            }
        }, 1, 1, TimeUnit.SECONDS);
        exec.schedule(
            new Runnable() {
                @Override
                public void run() {
                    ss.onShutdown();
                    System.err.println(ss);
                    System.err.println(ss.getSize());

                    workerGroup.shutdownGracefully().syncUninterruptibly();
                    exec.shutdownNow();
                }
            }
        , 500, TimeUnit.SECONDS);
//        exec.shutdown();
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
                    ChannelFuture connectFuture = b.connect("localhost", 5000);
                    connectFuture.addListener(connectListener);
                    connectFuture.channel().closeFuture().addListener(this);
                }
            }
        };
        connectFuture.channel().closeFuture().addListener(closeListener);
    }
}
