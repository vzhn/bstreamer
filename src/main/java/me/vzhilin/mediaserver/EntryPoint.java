package me.vzhilin.mediaserver;

import me.vzhilin.mediaserver.conf.Config;
import me.vzhilin.mediaserver.server.RtspServer;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;

public class EntryPoint {
    public static void main(String... argv) throws IOException {
        new EntryPoint().start();
    }

    private void start() throws IOException {
        BasicConfigurator.configure();
        Config config = new Config(EntryPoint.class.getResourceAsStream("/settings.yaml"));
        RtspServer server = new RtspServer(config);
        server.start();
    }
}
