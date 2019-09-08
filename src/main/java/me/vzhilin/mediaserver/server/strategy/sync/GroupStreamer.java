package me.vzhilin.mediaserver.server.strategy.sync;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatchers;
import io.netty.channel.group.DefaultChannelGroup;
import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.InterleavedFrame;
import me.vzhilin.mediaserver.media.impl.file.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.server.ServerContext;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;
import me.vzhilin.mediaserver.util.scheduler.PushSource;
import me.vzhilin.mediaserver.util.scheduler.PushSourceSession;
import me.vzhilin.mediaserver.util.scheduler.PushTaskSubscriber;
import me.vzhilin.mediaserver.util.scheduler.PushedPacket;

public final class GroupStreamer {
    private final ChannelGroup group;
    private final PropertyMap sourceConfig;
    private final ServerStatistics stat;
    private final EventLoopGroup loopGroup;
    private final ChannelGroupWritabilityMonitor groupWritabilityMonitor;

    private final PushSource pushSource;
    private final PushTaskSubscriber sub;
    private PushedPacket delayedPacket;
    private PushSourceSession pushSession;

    public GroupStreamer(ServerContext context, EventLoopGroup loopGroup, PushSource source) {
        this.pushSource = source;
        this.sourceConfig = source.getProps();
        this.loopGroup = loopGroup;
        this.group = new DefaultChannelGroup(loopGroup.next());
        this.stat = context.getStat();

        groupWritabilityMonitor = new ChannelGroupWritabilityMonitor(this::onWritable, this::onUnwritable);
        sub = new PushTaskSubscriber() {
            @Override
            public void onNext(PushedPacket pp) {
                GroupStreamer.this.onNext(pp);
            }

            @Override
            public void onEnd() {
                GroupStreamer.this.onEnd();
            }
        };
    }

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

    public void detachContext(ChannelHandlerContext context) {
        boolean wasLast = group.remove(context.channel()) & group.isEmpty();
        if (wasLast) {
            stopPlaying();
        }
        stat.closeConn(sourceConfig);
    }

    public MediaPacketSourceDescription describe() {
        return pushSource.describe();
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
        pushSession = pushSource.subscribe(sub);
    }

    private void stopPlaying() {
        if (pushSession != null) {
            pushSession.close();
            pushSession = null;
            if (delayedPacket != null) {
                delayedPacket.drain().release();
                delayedPacket = null;
            }
            group.close();
        }
    }

    private void send(PushedPacket pp) {
        if (groupWritabilityMonitor.isWritable()) {
            final int channels = group.size();
            InterleavedFrame interleaved = pp.drain();
            long bytes = (long) interleaved.getSize() * channels;
            stat.incByteCount(sourceConfig, bytes);
            group.writeAndFlush(interleaved, ChannelMatchers.all(), true);
            interleaved.release();
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
