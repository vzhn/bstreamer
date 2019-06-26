package me.vzhilin.mediaserver;

import me.vzhilin.mediaserver.server.RtspServer;
import org.apache.log4j.BasicConfigurator;

public class Mediaserver {
    public static void main(String... argv) {
        new Mediaserver().start();
    }

    private void start() {
        BasicConfigurator.configure();
        new RtspServer().start();
    }
}
