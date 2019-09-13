package me.vzhilin.bstreamer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import me.vzhilin.bstreamer.client.ClientAttributes;
import me.vzhilin.bstreamer.client.ClientReporter;
import me.vzhilin.bstreamer.client.ConnectionStatistics;
import me.vzhilin.bstreamer.client.TotalStatistics;
import me.vzhilin.bstreamer.client.conf.ClientConfig;
import me.vzhilin.bstreamer.client.conf.ConnectionSettings;
import me.vzhilin.bstreamer.client.conf.NetworkOptions;
import me.vzhilin.bstreamer.client.handler.ClientChannelInitializer;
import org.apache.commons.cli.*;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.util.List;

public class ClientCLI {
    static {
        BasicConfigurator.configure();
    }
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
        if (cmd.getOptions().length == 0 || cmd.hasOption("help")) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("bclient [options]", options);
        } else {
            String configPath = cmd.getOptionValue("config");
            if (configPath == null || configPath.isEmpty()) {
                System.err.println("config file not found!");
                return;
            }
            if (cmd.hasOption('l')) {
                String loglevel = cmd.getOptionValue('l');
                Logger.getRootLogger().setLevel(Level.toLevel(loglevel));
            } else {
                Logger.getRootLogger().setLevel(Level.INFO);
            }
            ClientConfig conf = ClientConfig.read(new File(configPath));
            ClientCLI client = new ClientCLI(conf);
            client.start(conf.getConnections());
        }
    }

    public ClientCLI(ClientConfig conf) {
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
