package me.vzhilin.mediaserver.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class RtspServer {
    public void start() {
        ServerBootstrap bootstrap = new ServerBootstrap();

        bootstrap.group(new NioEventLoopGroup(4))
                .channel(NioServerSocketChannel.class)
                .childHandler(new RtspServerInitializer())
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        try {
            ChannelFuture future = bootstrap.bind(5000).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
