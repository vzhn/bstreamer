package me.vzhilin.mediaserver.server.strategy.sync;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class SyncStrategy implements StreamingStrategy {
    private final static Logger LOG = Logger.getLogger(SyncStrategy.class);

    private final ChannelGroup group;
    private final PropertyMap sourceConfig;
    private final MediaPacketSourceFactory sourceFactory;
    private final ServerStatistics stat;
    private final ServerContext context;
    private final EventLoopGroup loopGroup;
    private final BufferingLimits limits;
    private final ChannelGroupWritabilityMonitor groupWritabilityMonitor;
    private final ScheduledExecutorService workerExecutor;

    private PushSource pushSource;
    private PushedPacket delayedPacket;

    public SyncStrategy(ServerContext context, EventLoopGroup loopGroup, PropertyMap sourceConfig) {
        String sourceName = sourceConfig.getValue(CommonSourceAttributes.NAME);
        this.workerExecutor = Executors.newScheduledThreadPool(4);
        this.loopGroup = loopGroup;
        this.context = context;
        this.sourceConfig = sourceConfig;
        this.sourceFactory = context.getSourceFactory(sourceName);
        this.group = new DefaultChannelGroup(loopGroup.next());
        this.stat = context.getStat();

        PropertyMap props = context.getConfig().getStrategyConfig("sync");
        int sizeLimit = props.getInt(SyncStrategyAttributes.LIMIT_SIZE);
        int packetLimit = props.getInt(SyncStrategyAttributes.LIMIT_PACKETS);
        int timeLimit = props.getInt(SyncStrategyAttributes.LIMIT_TIME);
        limits = new BufferingLimits(sizeLimit, packetLimit, timeLimit);

        groupWritabilityMonitor = new ChannelGroupWritabilityMonitor(this::onWritable, this::onUnwritable);
    }

    @Override
    public void attachContext(ChannelHandlerContext ctx) {
        Channel ch = ctx.channel();
        ch.closeFuture().addListener((ChannelFutureListener) future -> detachContext(ctx));
        ch.pipeline().addLast("group_writability_monitor", groupWritabilityMonitor);

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

    private void onWritable() {
        if (delayedPacket != null && !group.isEmpty()) {
            PushedPacket local = delayedPacket;
            delayedPacket = null;
            send(local);
        }
    }

    private void onUnwritable() { }

    private void startPlaying() {
        PullSource pullSource = sourceFactory.newSource(context, sourceConfig);
        pushSource = new PushSource(pullSource, limits, workerExecutor, this::onNext, this::onEnd);
        pushSource.start();
    }

    private void stopPlaying() {
        try {
            pushSource.stop();
            if (delayedPacket != null) {
                delayedPacket.drain().getPayload().release();
            }
        } catch (IOException e) {
            LOG.error(e, e);
        }
        group.close();
    }

    private void send(PushedPacket pp) {
        if (groupWritabilityMonitor.isWritable()) {
            final int channels = group.size();
            InterleavedFrame interleaved = pp.drain();
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
            delayedPacket = pp;
        }
    }

    private void onNext(PushedPacket pp) {
        loopGroup.execute(() -> send(pp));
    }

    private void onEnd() {
        loopGroup.execute(this::stopPlaying);
    }
}
