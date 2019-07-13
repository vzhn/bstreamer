package me.vzhilin.mediaserver;

import me.vzhilin.mediaserver.conf.Config;
import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.server.RtspServer;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;
import java.io.InputStream;

public class EntryPoint {
    public static void main(String... argv) throws IOException {
        new EntryPoint().start();
    }

    private void start() throws IOException {
        BasicConfigurator.configure();
        InputStream yamlResource = EntryPoint.class.getResourceAsStream("/settings.yaml");
        Config config = new Config(PropertyMap.parseYaml(yamlResource));
        RtspServer server = new RtspServer(config);
        server.start();
    }
}
