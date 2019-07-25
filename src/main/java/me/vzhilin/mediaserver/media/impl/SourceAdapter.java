package me.vzhilin.mediaserver.media.impl;

import me.vzhilin.mediaserver.media.PullSource;
import me.vzhilin.mediaserver.media.PushSource;
import me.vzhilin.mediaserver.media.PushSourceListener;
import me.vzhilin.mediaserver.media.impl.file.MediaPacket;
import me.vzhilin.mediaserver.media.impl.file.MediaPacketSourceDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class SourceAdapter implements PushSource {
    private final PullSource delegate;
    private final ExecutorService executor;
    private final Runnable command;
    private List<PushSourceListener> listeners = new ArrayList<>();
    private Future<?> future;

    public SourceAdapter(ExecutorService executor, PullSource delegate) {
        this.delegate = delegate;
        this.executor = executor;
        command = new PopulateTask(delegate);
    }

    @Override
    public MediaPacketSourceDescription getDesc() {
        return delegate.getDesc();
    }

    @Override
    public void subscribe(PushSourceListener listener) {
        if (listeners.isEmpty() & listeners.add(listener)) {
            future = executor.submit(command);
        }
    }

    @Override
    public void unsubscribe(PushSourceListener listener) {
        if (this.listeners.remove(listener) & listeners.isEmpty()) {
            future.cancel(true);
        }
    }

    private class PopulateTask implements Runnable {
        private final PullSource delegate;

        public PopulateTask(PullSource delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            if (delegate.hasNext()) {
                MediaPacket pkt = delegate.next();
                sendPacket(pkt);
            } else {
                sendEof();
            }
        }
    }

    private void sendPacket(MediaPacket pkt) {
        for (PushSourceListener listener: listeners) {
            listener.onNext(pkt);
        }
    }

    private void sendEof() {
        for (PushSourceListener listener: listeners) {
            listener.onEof();
        }
    }
}
