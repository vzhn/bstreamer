package me.vzhilin.bstreamer.server.scheduler;

import me.vzhilin.bstreamer.server.streaming.base.PullSource;
import me.vzhilin.bstreamer.server.streaming.file.SourceDescription;
import me.vzhilin.bstreamer.util.PropertyMap;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

public final class PushSource {
    private final ScheduledExecutorService pullExecutor;
    private final PushTask task;
    private final PropertyMap props;

    private int subscribers;
    private Future<?> pushTaskFuture;

    public PushSource(Supplier<PullSource> pullSourceSupplier,
                      PropertyMap props,
                      ScheduledExecutorService pullExecutor,
                      BufferingLimits bufferingLimits) {
        this.pullExecutor = pullExecutor;
        this.props = props;
        int maxRtpSize = props.getInt("max_rtp_size", 65536);
        task = new PushTask(pullSourceSupplier, bufferingLimits, maxRtpSize, pullExecutor);
    }

    public SourceDescription describe() {
        return task.describe();
    }

    public PushSourceSession subscribe(PushTaskSubscriber sub) {
        synchronized (this) {
            PushTaskSession s = task.subscribe(sub);
            if (++subscribers == 1) {
                pushTaskFuture = pullExecutor.submit(task);
            }
            return new PushSourceSession(() -> {
                s.close();
                synchronized (PushSource.this) {
                    if (--subscribers == 0) {
                        pushTaskFuture.cancel(false);
                    }
                }
            });
        }
    }

    public PropertyMap getProps() {
        return props;
    }
}
