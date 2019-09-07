package me.vzhilin.mediaserver.client.conf;

import java.util.Optional;

public class NetworkOptions {
    /** Netty worker threads */
    private Integer threads;

    /** SO_RCVBUF */
    private Integer rcvbuf;

    /** Connect timeout millis */
    private Integer connectTimeout;

    /** Connection idle timeout */
    private Integer idleTimeout;

    public Optional<Integer> getRcvbuf() {
        return Optional.ofNullable(rcvbuf);
    }

    public Optional<Integer> getThreads() {
        return Optional.ofNullable(threads);
    }

    public Optional<Integer> getConnectTimeout() {
        return Optional.ofNullable(connectTimeout);
    }

    public Optional<Integer> getIdleTimeout() {
        return Optional.ofNullable(idleTimeout);
    }
}

