package me.vzhilin.mediaserver;

import me.vzhilin.mediaserver.conf.Config;
import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.server.RtspServer;
import org.apache.commons.cli.*;
import org.apache.log4j.BasicConfigurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MediaserverCLI {
    private final CommandLine cmd;
    private final Options options;

    public static void main(String... argv) throws IOException, ParseException {
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
            server.start();
        }
    }
}
