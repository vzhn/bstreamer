package me.vzhilin.mediaserver;

import me.vzhilin.mediaserver.server.RtspServer;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;

public class MediaserverDaemon implements Daemon {
    private RtspServer instance;

    @Override
    public void init(DaemonContext daemonContext) {
        instance = new RtspServer();
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
