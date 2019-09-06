package me.vzhilin.mediaserver.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.util.AttributeKey;
import me.vzhilin.mediaserver.client.conf.ClientConfig;
import me.vzhilin.mediaserver.client.conf.ConnectionSettings;
import me.vzhilin.mediaserver.client.conf.NetworkOptions;
import me.vzhilin.mediaserver.client.handler.ClientChannelInitializer;
import org.apache.log4j.BasicConfigurator;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;

public class Client {
    private static final AttributeKey<ConnectionStatistics> STAT = AttributeKey.valueOf("stat");
    public static final AttributeKey<URI> URL = AttributeKey.valueOf("url");

    private final Bootstrap b;
    private final TotalStatistics ss;
    private final ClientConfig conf;

    private final ChannelFutureListener ON_CLOSED = new ClosedListener();
    private final ChannelFutureListener ON_CONNECTED = new ConnectedListener();

    public static void main(String... argv) throws IOException {
        BasicConfigurator.configure();
        ClientConfig conf = ClientConfig.read(new File(argv[0]));
        Client client = new Client(conf);
        client.start();
    }

    public Client(ClientConfig conf) {
        this.conf = conf;
        this.ss = new TotalStatistics();

        NetworkOptions nw = conf.getNetwork();
        final int nThreads = nw.getThreads().orElse(Runtime.getRuntime().availableProcessors() * 2);
        b = new Bootstrap()
            .group(new EpollEventLoopGroup(nThreads))
            .channel(EpollSocketChannel.class);
        nw.getRcvbuf().ifPresent(rcvbuf -> b.option(ChannelOption.SO_RCVBUF, rcvbuf));
        b.handler(new ClientChannelInitializer(ss));
    }

    public void start() {
        System.err.println("pid = " + ManagementFactory.getRuntimeMXBean().getName());
        for (ConnectionSettings conn: conf.getConnections()) {
            URI uri = URI.create(conn.getUrl());
            int n = conn.getN();
            for (int i = 0; i < n; i++) {
                ConnectionStatistics stat = ss.newStat();
                Bootstrap btstrp = b.clone().attr(STAT, stat);
                btstrp.attr(URL, uri);
                btstrp.connect(uri.getHost(), uri.getPort()).addListener(ON_CONNECTED);
            }
        }

        startReporter(ss);
    }

    private void startReporter(TotalStatistics ss) {
        new ClientReporter(ss).start();
    }

    private final class ConnectedListener implements ChannelFutureListener {
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
                    b.connect(uri.getHost(), uri.getPort()).addListener(ConnectedListener.this);
                }
            }
        }
    }

    private final class ClosedListener implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future) {
            Channel channel = future.channel();
            if (!channel.eventLoop().isShuttingDown()) {
                URI uri = channel.attr(URL).get();
                b.connect(uri.getHost(), uri.getPort()).addListener(ON_CONNECTED);
            }
        }
    }
}
