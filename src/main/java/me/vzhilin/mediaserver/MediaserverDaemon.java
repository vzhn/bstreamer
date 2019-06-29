package me.vzhilin.mediaserver;

import me.vzhilin.mediaserver.conf.Config;
import me.vzhilin.mediaserver.server.RtspServer;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;

import java.io.IOException;

public class MediaserverDaemon implements Daemon {
    private RtspServer instance;

    @Override
    public void init(DaemonContext daemonContext) throws IOException {
        Config config = new Config(EntryPoint.class.getResourceAsStream("/settings.yaml"));
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
