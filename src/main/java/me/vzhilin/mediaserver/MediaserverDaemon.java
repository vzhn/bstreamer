package me.vzhilin.mediaserver;

import me.vzhilin.mediaserver.server.RtspServer;
import me.vzhilin.mediaserver.server.conf.Config;
import me.vzhilin.mediaserver.util.PropertyMap;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;

public class MediaserverDaemon implements Daemon {
    private RtspServer instance;

    @Override
    public void init(DaemonContext daemonContext) throws IOException {
        BasicConfigurator.configure();
        Config config = new Config(PropertyMap.parseYaml(EntryPoint.class.getResourceAsStream("/server.yaml")));
        instance = new RtspServer(config);
    }

    @Override
    public void start() {
        instance.start();
    }

    @Override
    public void stop() {
        instance.stop();
    }

    @Override
    public void destroy() {

    }
}
