package me.vzhilin.bstreamer;

import me.vzhilin.bstreamer.server.RtspServer;
import me.vzhilin.bstreamer.server.ServerContext;
import me.vzhilin.bstreamer.server.ServerReporter;
import me.vzhilin.bstreamer.server.conf.Config;
import me.vzhilin.bstreamer.server.stat.ServerStatistics;
import me.vzhilin.bstreamer.util.ConfigLocator;
import me.vzhilin.bstreamer.util.PropertyMap;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ServerCLI {
    private final static Logger LOG = LogManager.getLogger(ServerCLI.class);

    private final CommandLine cmd;
    private final Options options;

    public static void main(String... argv) throws IOException, ParseException {
        ServerCLI server = new ServerCLI(argv);
        server.start();
    }

    private ServerCLI(String[] argv) throws ParseException {
        options = new Options();
        options.addOption("h", "help", false, "show help and exit");
        options.addOption("c", "config", true, "config file");
        options.addOption("l", "loglevel", true, "log level [OFF|FATAL|ERROR|WARN|INFO|DEBUG|TRACE|ALL]");
        CommandLineParser parser = new DefaultParser();
        cmd = parser.parse(options, argv);
    }

    private void start() throws IOException {
        if (cmd.hasOption("help")) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("bserver [options]", options);
        } else {
            if (cmd.hasOption('l')) {
                String loglevel = cmd.getOptionValue('l');
                Configurator.setRootLevel(Level.toLevel(loglevel));
            } else {
                Configurator.setRootLevel(Level.INFO);
            }

            Optional<File> configPath = new ConfigLocator("server.yaml").locate(cmd.getOptionValue("config"));
            if (!configPath.isPresent()) {
                System.exit(1);
            }
            Path path = configPath.get().toPath();
            LOG.info("config loaded: {}", path);
            InputStream is = Files.newInputStream(path);
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
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        new ServerReporter(stat, exec).start();
    }
}
