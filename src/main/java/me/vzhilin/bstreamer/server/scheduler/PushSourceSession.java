package me.vzhilin.bstreamer.server.scheduler;

public final class PushSourceSession {
    private final Runnable unsubscribe;
    private boolean closed;

    public PushSourceSession(Runnable unsubscribe) {
        this.unsubscribe = unsubscribe;
    }

    public void close() {
        boolean wasClosed;
        synchronized (this) {
            if (!closed) {
                wasClosed = true;
                closed = true;
            } else {
                wasClosed = false;
            }
        }

        if (wasClosed) {
            unsubscribe.run();
        }
    }
}
