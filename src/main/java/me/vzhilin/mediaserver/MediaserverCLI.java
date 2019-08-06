package me.vzhilin.mediaserver;

import io.netty.channel.EventLoopGroup;
import me.vzhilin.mediaserver.conf.Config;
import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.server.ConsoleReporter;
import me.vzhilin.mediaserver.server.RtspServer;
import me.vzhilin.mediaserver.server.ServerContext;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;
import org.apache.commons.cli.*;
import org.apache.log4j.BasicConfigurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;

public class MediaserverCLI {
    private final CommandLine cmd;
    private final Options options;

    public static void main(String... argv) throws IOException, ParseException {
        System.err.println("pid = " + ManagementFactory.getRuntimeMXBean().getName());

        BasicConfigurator.configure();
        MediaserverCLI mediaserver = new MediaserverCLI(argv);
        mediaserver.start();
    }

    private MediaserverCLI(String[] argv) throws ParseException {
        options = new Options();
        options.addOption("h", "help", false, "show help and exit");
        options.addOption("c", "config", true, "config file");
        options.addOption("l", "loglevel", true, "log level [info|debug|trace]");
        CommandLineParser parser = new DefaultParser();
        cmd = parser.parse(options, argv);
    }

    private void start() throws IOException {
        if (cmd.getOptions().length == 0 || cmd.hasOption("help")) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("Mediaserver", options);
        } else {
            String configPath = cmd.getOptionValue("config");
            if (configPath == null || configPath.isEmpty()) {
                System.err.println("config file not found!");
                System.exit(1);
            }
            File configFile = new File(configPath);
            if (!configFile.exists()) {
                System.err.println("config file not found!");
                System.exit(1);
            }
            InputStream is = new FileInputStream(configPath);
            PropertyMap yaml = PropertyMap.parseYaml(is);
            Config config = new Config(yaml);
            RtspServer server = new RtspServer(config);
            startConsoleReporter(server);
            server.start();
        }
    }

    private void startConsoleReporter(RtspServer server) {
        ServerContext sc = server.getServerContext();
        ServerStatistics stat = sc.getStat();
        EventLoopGroup exec = sc.getScheduledExecutor();
        new ConsoleReporter(stat, exec).start();
    }
}
