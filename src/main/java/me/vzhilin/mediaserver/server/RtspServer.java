package me.vzhilin.mediaserver.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class RtspServer {
    public void start() {
        ServerBootstrap bootstrap = new ServerBootstrap();

        bootstrap.group(new EpollEventLoopGroup(1), new EpollEventLoopGroup(2))
                .channel(EpollServerSocketChannel.class)
                .childHandler(new RtspServerInitializer())
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(64 * 1024, 256 * 1024))
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.SO_SNDBUF, 256 * 1024);

        try {
            ChannelFuture future = bootstrap.bind(5000).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
