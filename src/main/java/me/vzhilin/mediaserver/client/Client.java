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
import java.util.List;

public class Client {
    private static final AttributeKey<ConnectionStatistics> STAT = AttributeKey.valueOf("stat");
    public static final AttributeKey<URI> URL = AttributeKey.valueOf("url");

    private final Bootstrap bootstrap;
    private final ClientConfig conf;
    private final TotalStatistics ss;

    private final ChannelFutureListener ON_CLOSED = new ClosedListener();
    private final ChannelFutureListener ON_CONNECTED = new ConnectedListener();

    public static void main(String... argv) throws IOException {
        BasicConfigurator.configure();
        ClientConfig conf = ClientConfig.read(new File(argv[0]));
        Client client = new Client(conf);
        client.start(conf.getConnections());
    }

    public Client(ClientConfig conf) {
        this.ss = new TotalStatistics();
        this.conf = conf;

        NetworkOptions nw = conf.getNetwork();
        final int nThreads = nw.getThreads().orElse(Runtime.getRuntime().availableProcessors() * 2);
        bootstrap = new Bootstrap()
            .group(new EpollEventLoopGroup(nThreads))
            .channel(EpollSocketChannel.class);
        nw.getRcvbuf().ifPresent(rcvbuf -> bootstrap.option(ChannelOption.SO_RCVBUF, rcvbuf));
        nw.getConnectTimeout().ifPresent(timeout -> bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout));
        bootstrap.handler(new ClientChannelInitializer(this));
    }

    public void start(List<ConnectionSettings> connections) {
        System.err.println("pid = " + ManagementFactory.getRuntimeMXBean().getName());
        for (ConnectionSettings conn: connections) {
            URI uri = URI.create(conn.getUrl());
            int n = conn.getN();
            for (int i = 0; i < n; i++) {
                ConnectionStatistics stat = ss.newStat();
                bootstrap.clone()
                    .attr(STAT, stat)
                    .attr(URL, uri)
                    .connect(uri.getHost(), uri.getPort())
                    .addListener(ON_CONNECTED);
            }
        }

        startReporter(ss);
    }

    public TotalStatistics getStatistic() {
        return ss;
    }

    public ClientConfig getConf() {
        return conf;
    }

    private void startReporter(TotalStatistics ss) {
        new ClientReporter(ss).start();
    }

    private void connect(Channel channel) {
        URI uri = channel.attr(URL).get();
        ConnectionStatistics connectionStat = channel.attr(STAT).get();
        bootstrap
            .attr(URL, uri)
            .attr(STAT, connectionStat)
            .connect(uri.getHost(), uri.getPort()).addListener(ON_CONNECTED);
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
                    connect(channel);
                }
            }
        }
    }

    private final class ClosedListener implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future) {
            Channel channel = future.channel();
            if (!channel.eventLoop().isShuttingDown()) {
                connect(channel);
            }
        }
    }
}
