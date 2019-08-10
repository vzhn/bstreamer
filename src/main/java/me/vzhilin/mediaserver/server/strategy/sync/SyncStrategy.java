package me.vzhilin.mediaserver.server.strategy.sync;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatchers;
import io.netty.channel.group.DefaultChannelGroup;
import me.vzhilin.mediaserver.InterleavedFrame;
import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.PullSource;
import me.vzhilin.mediaserver.media.impl.CommonSourceAttributes;
import me.vzhilin.mediaserver.media.impl.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.media.impl.file.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.server.ServerContext;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;
import me.vzhilin.mediaserver.util.scheduler.BufferingLimits;
import me.vzhilin.mediaserver.util.scheduler.PushSource;
import me.vzhilin.mediaserver.util.scheduler.PushedPacket;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class SyncStrategy implements StreamingStrategy {
    private final static Logger LOG = Logger.getLogger(SyncStrategy.class);

    private final ChannelGroup group;
    private final PropertyMap sourceConfig;
    private final MediaPacketSourceFactory sourceFactory;
    private final ServerStatistics stat;
    private final PushListener packetListener;
    private final ServerContext context;
    private final EventLoopGroup executor;
    private final BufferingLimits limits;
    private final GroupWritabilityMonitor groupWritabilityMonitor;
    private final ScheduledExecutorService workerExecutor;

    private PushSource buffered;
    private PushedPacket delayedFrame;

    public SyncStrategy(ServerContext context, EventLoopGroup executor, PropertyMap sourceConfig) {
        String sourceName = sourceConfig.getValue(CommonSourceAttributes.NAME);
        this.workerExecutor = Executors.newScheduledThreadPool(4);
        this.executor = executor;
        this.context = context;
        this.sourceConfig = sourceConfig;
        this.sourceFactory = context.getSourceFactory(sourceName);
        this.group = new DefaultChannelGroup(executor.next());
        this.stat = context.getStat();
        packetListener = new PushListener();

        PropertyMap props = context.getConfig().getStrategyConfig("sync");
        int sizeLimit = props.getInt(SyncStrategyAttributes.LIMIT_SIZE);
        int packetLimit = props.getInt(SyncStrategyAttributes.LIMIT_PACKETS);
        int timeLimit = props.getInt(SyncStrategyAttributes.LIMIT_TIME);
        limits = new BufferingLimits(sizeLimit, packetLimit, timeLimit);

        groupWritabilityMonitor = new GroupWritabilityMonitor(this::onWritable, this::onUnwritable);
    }

    private void onWritable() {
        if (delayedFrame != null && !group.isEmpty()) {
            PushedPacket local = delayedFrame;
            delayedFrame = null;
            send(local);
        }
    }

    private void onUnwritable() { }

    @Override
    public void attachContext(ChannelHandlerContext ctx) {
        Channel ch = ctx.channel();
        ch.closeFuture().addListener((ChannelFutureListener) future -> detachContext(ctx));
        ch.pipeline().addLast("writability_monitor", groupWritabilityMonitor);

        group.add(ch);
        groupWritabilityMonitor.channelRegistered(ctx);
        stat.openConn(sourceConfig);
        if (group.size() == 1) {
            startPlaying();
        }
    }

    @Override
    public void detachContext(ChannelHandlerContext context) {
        boolean wasLast = group.remove(context.channel()) & group.isEmpty();
        if (wasLast) {
            stopPlaying();
        }
        stat.closeConn(sourceConfig);
    }

    @Override
    public MediaPacketSourceDescription describe() {
        PullSource src = sourceFactory.newSource(context, sourceConfig);
        MediaPacketSourceDescription desc = src.getDesc();
        try {
            src.close();
        } catch (IOException e) {
            LOG.error(e, e);
        }
        return desc;
    }

    private void startPlaying() {
        PullSource pullSource = sourceFactory.newSource(context, sourceConfig);
        buffered = new PushSource(pullSource, packetListener, limits, workerExecutor);
        buffered.start();
    }

    private void stopPlaying() {
        try {
            buffered.stop();
            if (delayedFrame != null) {
                delayedFrame.drain().getPayload().release();
            }
        } catch (IOException e) {
            LOG.error(e, e);
        }
        group.close();
    }

    private void send(PushedPacket buffered) {
        if (groupWritabilityMonitor.isWritable()) {
            final int channels = group.size();
            InterleavedFrame interleaved = buffered.drain();
            ByteBuf payload = interleaved.getPayload();
            if (channels == 0) {
                payload.release();
                return;
            } else
            if (channels > 1) {
                payload.retain(channels - 1);
            }
            long bytes = (long) payload.readableBytes() * channels;
            stat.incByteCount(sourceConfig, bytes);
            group.writeAndFlush(interleaved, ChannelMatchers.all(), true);
        } else {
            stat.incLateCount(sourceConfig);
            delayedFrame = buffered;
        }
    }

    @ChannelHandler.Sharable
    private final static class GroupWritabilityMonitor extends ChannelInboundHandlerAdapter {
        private final Runnable onWritable;
        private final Runnable onUnwritable;
        private Set<ChannelHandlerContext> unwritable = new HashSet<>();
        private volatile boolean writable;
        private int totalChannels;

        private GroupWritabilityMonitor(Runnable onWritable, Runnable onUnwritable) {
            this.onWritable = onWritable;
            this.onUnwritable = onUnwritable;
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) {
            ++totalChannels;
            if (!ctx.channel().isWritable()) {
                addToUnwritable(ctx);
            } else {
                if (!writable && unwritable.isEmpty()) {
                    writable = true;
                    onUnwritable.run();
                }
            }
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) {
            --totalChannels;
            removeFromUnwritable(ctx);
            if (totalChannels == 0 && writable) {
                writable = false;
                onUnwritable.run();
            }
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) {
            if (ctx.channel().isWritable()) {
                removeFromUnwritable(ctx);
            } else {
                addToUnwritable(ctx);
            }
        }

        private void addToUnwritable(ChannelHandlerContext ctx) {
            unwritable.add(ctx);
            if (writable) {
                writable = false;
                onUnwritable.run();
            }
        }

        private void removeFromUnwritable(ChannelHandlerContext ctx) {
            unwritable.remove(ctx);
            if (!writable && unwritable.isEmpty()) {
                writable = true;
                onWritable.run();
            }
        }

        private boolean isWritable() {
            return writable;
        }
    }

    private final class PushListener implements me.vzhilin.mediaserver.util.scheduler.PushListener {
        @Override
        public void next(PushedPacket scheduledMediaPacket) {
            executor.execute(() -> send(scheduledMediaPacket));
        }

        @Override
        public void end() {
            executor.execute(SyncStrategy.this::stopPlaying);
        }
    }
}
