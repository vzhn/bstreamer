package me.vzhilin.mediaserver.server.scheduler;

import me.vzhilin.mediaserver.server.media.PullSource;
import me.vzhilin.mediaserver.server.media.impl.file.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.util.PropertyMap;

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
                      BufferingLimits limits) {
        this.pullExecutor = pullExecutor;
        this.props = props;
        task = new PushTask(pullSourceSupplier, limits, pullExecutor);
    }

    public MediaPacketSourceDescription describe() {
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
