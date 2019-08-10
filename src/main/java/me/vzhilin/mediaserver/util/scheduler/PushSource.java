package me.vzhilin.mediaserver.util.scheduler;

import me.vzhilin.mediaserver.media.PullSource;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

public final class PushSource {
    private final PullSource pullSource;
    private final ScheduledExecutorService pullExecutor;
    private final PushTask task;

    private boolean started;
    private boolean stopped;

    public PushSource(PullSource pullSource,
                      PushListener listener,
                      BufferingLimits limits,
                      ScheduledExecutorService pullExecutor) {
        this.pullSource = pullSource;
        this.pullExecutor = pullExecutor;
        task = new PushTask(pullSource, limits, pullExecutor, listener);
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
