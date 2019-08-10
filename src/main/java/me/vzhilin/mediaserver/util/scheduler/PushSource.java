package me.vzhilin.mediaserver.util.scheduler;

import me.vzhilin.mediaserver.media.PullSource;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public final class PushSource {
    private final PullSource pullSource;
    private final ScheduledExecutorService pullExecutor;
    private final PushTask task;

    private boolean started;
    private boolean stopped;

    public PushSource(PullSource pullSource,
                      BufferingLimits limits,
                      ScheduledExecutorService pullExecutor,
                      Consumer<PushedPacket> onNext,
                      Runnable onEnd) {
        this.pullSource = pullSource;
        this.pullExecutor = pullExecutor;
        task = new PushTask(pullSource, limits, pullExecutor, onNext, onEnd);
    }

    public void start() {
        if (!started) {
            started = true;
            pullExecutor.execute(task);
        }
    }

    public void stop() throws IOException {
        if (!stopped) {
            stopped = true;
            synchronized (task) {
                task.finish();
                pullSource.close();
            }
        }
    }
}
