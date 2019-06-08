package me.vzhilin.mediaserver.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;

public class Client {
    public static void main(String... argv) throws InterruptedException {
        new Client().start();
    }

    public void start() throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();

        EpollEventLoopGroup workerGroup = new EpollEventLoopGroup(1);

        bootstrap
                .group(workerGroup)
                .channel(EpollSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();

                        pipeline.addLast(new RtspInterleavedDecoder());
                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new ClientHandler());
                    }
                })
                .connect("localhost", 5000).sync();
    }
}
