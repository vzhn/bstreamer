package me.vzhilin.mediaserver;

import me.vzhilin.mediaserver.conf.Config;
import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.server.RtspServer;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

public class EntryPoint {
    private static EntryPoint INSTANCE;
    private RtspServer server;

    public static void main(String... argv) throws IOException {
        start();
    }

    private synchronized static EntryPoint getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new EntryPoint();
        }
        return INSTANCE;
    }

    public static void start(String... args) throws IOException {
        getInstance().startServer();
    }

    public static void stop(String... args) throws IOException {
        getInstance().stopServer();
    }

    private EntryPoint() {
        BasicConfigurator.configure();
        Logger.getLogger("io.netty").setLevel(Level.INFO);
    }

    private synchronized void startServer() throws IOException {
        InputStream yamlResource = EntryPoint.class.getResourceAsStream("/settings.yaml");
        Config config = new Config(PropertyMap.parseYaml(yamlResource));
        server = new RtspServer(config);
        server.start();
    }

    private synchronized void stopServer() {
        server.stop();
    }
}
