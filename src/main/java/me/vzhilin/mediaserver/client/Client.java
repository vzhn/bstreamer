package me.vzhilin.mediaserver.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.util.AttributeKey;
import org.apache.log4j.BasicConfigurator;

import java.util.HashSet;

public class Client {
    private HashSet<ConnectionStatistics> stats;

    public static void main(String... argv) throws InterruptedException {
        Client client = new Client();
        client.start();
    }

    public void start() throws InterruptedException {
        stats = new HashSet<>();

        BasicConfigurator.configure();
        Bootstrap bootstrap = new Bootstrap();

        EpollEventLoopGroup workerGroup = new EpollEventLoopGroup(4);
        ClientStatistics statistics = new ClientStatistics();

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
                    pipeline.addLast(new ClientHandler(statistics));
                }
            });


        for (int i = 0; i < 1000; i++) {
            ConnectionStatistics stat = new ConnectionStatistics();
            stats.add(stat);

            Bootstrap btstrp = b.clone();
            ChannelFuture future = btstrp.connect("localhost", 5000);

            bindListener(btstrp, future, stat);
        }
    }

    private void bindListener(Bootstrap b, ChannelFuture connectFuture, ConnectionStatistics stat) {
        ChannelFutureListener connectListener = new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) {
                future.channel().attr(AttributeKey.valueOf("stat")).set(stat);

                stat.onConnected();
            }
        };
        connectFuture.addListener(connectListener);

        ChannelFutureListener closeListener = new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                stat.onDisconnected();

                ChannelFuture connectFuture = b.connect("localhost", 5000);
                connectFuture.addListener(connectListener);
                connectFuture.channel().closeFuture().addListener(this);
            }
        };
        connectFuture.channel().closeFuture().addListener(closeListener);
    }

    private void printStat() {
        for (ConnectionStatistics stat: stats) {
            System.out.println(stat.getTotal());
        }
    }
}
