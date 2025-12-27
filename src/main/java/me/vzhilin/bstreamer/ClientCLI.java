package me.vzhilin.bstreamer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import me.vzhilin.bstreamer.client.ClientAttributes;
import me.vzhilin.bstreamer.client.ClientReporter;
import me.vzhilin.bstreamer.client.ConnectionStatistics;
import me.vzhilin.bstreamer.client.TotalStatistics;
import me.vzhilin.bstreamer.client.conf.ClientConfig;
import me.vzhilin.bstreamer.client.conf.ConnectionSettings;
import me.vzhilin.bstreamer.client.conf.NetworkOptions;
import me.vzhilin.bstreamer.client.handler.ClientChannelInitializer;
import me.vzhilin.bstreamer.util.AppRuntime;
import me.vzhilin.bstreamer.util.ConfigLocator;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

public class ClientCLI {
    private final Bootstrap bootstrap;
    private final ClientConfig conf;
    private final TotalStatistics ss;

    private final ChannelFutureListener ON_CLOSED = new ClosedListener();
    private final ChannelFutureListener ON_CONNECTED = new ConnectedListener();

    public static void main(String... argv) throws IOException, ParseException {
        Options options = new Options();
        options.addOption("h", "help", false, "show help and exit");
        options.addOption("c", "config", true, "config file");
        options.addOption("l", "loglevel", true, "log level [OFF|FATAL|ERROR|WARN|INFO|DEBUG|TRACE|ALL]");

        CommandLine cmd = new DefaultParser().parse(options, argv);
        if (cmd.hasOption("help")) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("bclient [options]", options);
        } else {
            if (cmd.hasOption('l')) {
                String loglevel = cmd.getOptionValue('l');
                Configurator.setRootLevel(Level.toLevel(loglevel));
            } else {
                Configurator.setRootLevel(Level.INFO);
            }
            Optional<File> configPath = new ConfigLocator("client.yaml").locate(cmd.getOptionValue("config"));
            if (!configPath.isPresent()) {
                System.exit(1);
            }
            ClientConfig conf = ClientConfig.read(configPath.get());
            ClientCLI client = new ClientCLI(conf);
            client.start(conf.getConnections());
        }
    }

    public ClientCLI(ClientConfig conf) {
        this.ss = new TotalStatistics();
        this.conf = conf;

        NetworkOptions nw = conf.getNetwork();
        final int nThreads = nw.getThreads().orElse(Runtime.getRuntime().availableProcessors() * 2);

        EventLoopGroup workers;
        Class<? extends SocketChannel> channelClazz;
        if (AppRuntime.IS_WINDOWS) {
            workers = new NioEventLoopGroup(nThreads);
            channelClazz = NioSocketChannel.class;
        } else {
            workers = new EpollEventLoopGroup(nThreads);
            channelClazz = EpollSocketChannel.class;
        }
        bootstrap = new Bootstrap()
            .group(workers)
            .channel(channelClazz);
        nw.getRcvbuf().ifPresent(rcvbuf -> bootstrap.option(ChannelOption.SO_RCVBUF, rcvbuf));
        nw.getConnectTimeout().ifPresent(timeout -> bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout));
        bootstrap.handler(new ClientChannelInitializer(this));
    }

    public void start(List<ConnectionSettings> connections) {
        for (ConnectionSettings conn: connections) {
            URI uri = URI.create(conn.getUrl());
            int n = conn.getN();
            for (int i = 0; i < n; i++) {
                ConnectionStatistics stat = ss.newStat();
                bootstrap.clone()
                    .attr(ClientAttributes.STAT, stat)
                    .attr(ClientAttributes.URL, uri)
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
        URI uri = channel.attr(ClientAttributes.URL).get();
        ConnectionStatistics connectionStat = channel.attr(ClientAttributes.STAT).get();
        bootstrap
            .attr(ClientAttributes.URL, uri)
            .attr(ClientAttributes.STAT, connectionStat)
            .connect(uri.getHost(), uri.getPort()).addListener(ON_CONNECTED);
    }

    private final class ConnectedListener implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future) {
            if (future.isSuccess()) {
                Channel channel = future.channel();
                ConnectionStatistics stat = channel.attr(ClientAttributes.STAT).get();
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
