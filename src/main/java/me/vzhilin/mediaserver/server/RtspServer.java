package me.vzhilin.mediaserver.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.sctp.nio.NioSctpServerChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactoryRegistry;
import me.vzhilin.mediaserver.server.strategy.seq.SequencedStrategyFactory;
import me.vzhilin.mediaserver.server.strategy.sync.SyncStrategyFactory;

public class RtspServer {
    public void start() {
        ServerBootstrap bootstrap = new ServerBootstrap();

        EventLoopGroup bossGroup = new EpollEventLoopGroup(1);
        EpollEventLoopGroup workerGroup = new EpollEventLoopGroup(1);
//        workerGroup.setIoRatio(100);

        StreamingStrategyFactoryRegistry register = new StreamingStrategyFactoryRegistry();
        register.addFactory("sync", new SyncStrategyFactory(workerGroup));
        register.addFactory("seq", new SequencedStrategyFactory());

//        final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

        bootstrap.group(bossGroup, workerGroup)
                .channel(EpollServerSocketChannel.class)
                .childHandler(new RtspServerInitializer(register))
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(256 * 1024, 512 * 1024))
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.SO_SNDBUF, 1 * 1024);

        try {
            ChannelFuture future = bootstrap.bind(5000).sync();

//            workerGroup.scheduleWithFixedDelay(new Runnable() {
//                TickEvent tick = new TickEvent();
//                @Override
//                public void run() {
//                    channels.forEach(new Consumer<Channel>() {
//                        @Override
//                        public void accept(Channel channel) {
//                            channel.pipeline().fireUserEventTriggered(tick);
//                        }
//                    });
//                }
//            }, 40, 40, TimeUnit.MILLISECONDS);

            future.channel().closeFuture().sync();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
