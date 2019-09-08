package me.vzhilin.mediaserver.server.scheduler;

public class PushTaskSession {
    private final Runnable close;
    private boolean closed;
    public PushTaskSession(Runnable close) {
        this.close = close;
    }

    public void close() {
        final boolean wasClosed;
        synchronized (this) {
            if (!closed) {
                closed = true;
                wasClosed = true;
            } else {
                wasClosed = false;
            }
        }

        if (wasClosed) {
            close.run();
        }
    }
}
