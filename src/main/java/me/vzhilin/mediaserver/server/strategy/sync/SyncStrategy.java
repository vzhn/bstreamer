package me.vzhilin.mediaserver.server.strategy.sync;

import io.netty.channel.ChannelHandlerContext;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SyncStrategy implements StreamingStrategy {
    /** mkv file */
    private final String fileName;

    /** client connections */
    private final List<ChannelHandlerContext> contexts = new ArrayList<>();

    /** executor */
    private final ScheduledExecutorService scheduledExecutor;

    private ScheduledFuture<?> streamingFuture;

    public SyncStrategy(String fileName, ScheduledExecutorService scheduledExecutor) {
        this.fileName = fileName;
        this.scheduledExecutor = scheduledExecutor;
    }

    @Override
    public void attachContext(ChannelHandlerContext context) {
        boolean wasFirst = contexts.isEmpty();
        contexts.add(context);

        if (wasFirst) {
            startPlaying();
        }
    }

    @Override
    public void detachContext(ChannelHandlerContext context) {
        boolean wasLast = contexts.remove(context) && contexts.isEmpty();
        if (wasLast) {
            stopPlaying();
        }
    }

    private void startPlaying() {
        streamingFuture = scheduledExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {

            }
        }, 0, 40, TimeUnit.MILLISECONDS);
    }

    private void stopPlaying() {
        streamingFuture.cancel(true);
    }
}
