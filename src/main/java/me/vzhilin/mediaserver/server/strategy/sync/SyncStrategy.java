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
import me.vzhilin.mediaserver.util.BufferedPacketSource;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public final class SyncStrategy implements StreamingStrategy {
    private final static Logger LOG = Logger.getLogger(SyncStrategy.class);

    private final ChannelGroup group;
    private final PropertyMap sourceConfig;
    private final MediaPacketSourceFactory sourceFactory;
    private final ServerStatistics stat;
    private final PacketListener packetListener;
    private final ServerContext context;
    private final EventLoopGroup executor;
    private final BufferedPacketSource.BufferingLimits limits;
    private final GroupWritabilityMonitor groupWritabilityMonitor;

    private BufferedPacketSource buffered;
    private BufferedPacketSource.BufferedMediaPacket delayedFrame;

    public SyncStrategy(ServerContext context, EventLoopGroup executor, PropertyMap sourceConfig) {
        String sourceName = sourceConfig.getValue(CommonSourceAttributes.NAME);
        this.executor = executor;
        this.context = context;
        this.sourceConfig = sourceConfig;
        this.sourceFactory = context.getSourceFactory(sourceName);
        this.group = new DefaultChannelGroup(executor.next());
        this.stat = context.getStat();
        packetListener = new PacketListener();

        PropertyMap props = context.getConfig().getStrategyConfig("sync");
        int sizeLimit = props.getInt(SyncStrategyAttributes.LIMIT_SIZE);
        int packetLimit = props.getInt(SyncStrategyAttributes.LIMIT_PACKETS);
        int timeLimit = props.getInt(SyncStrategyAttributes.LIMIT_TIME);
        limits = new BufferedPacketSource.BufferingLimits(sizeLimit, packetLimit, timeLimit);

        groupWritabilityMonitor = new GroupWritabilityMonitor();
    }

    private void onGroupWritable() {
//        System.err.println("writable!");
        if (delayedFrame != null && !group.isEmpty()) {
            BufferedPacketSource.BufferedMediaPacket local = delayedFrame;
            delayedFrame = null;
            send(local);
        }
    }

    private void onGroupNotWritable() {
//        System.err.println("not writable!");
    }

    @Override
    public void attachContext(ChannelHandlerContext ctx) {
        Channel ch = ctx.channel();

        boolean wasFirst = group.isEmpty();
        group.add(ch);
        ch.closeFuture().addListener((ChannelFutureListener) future -> detachContext(ctx));
        if (wasFirst) {
            startPlaying();
        }
        ch.pipeline().addLast("writability_monitor", groupWritabilityMonitor);
        groupWritabilityMonitor.channelRegistered(ctx);
        stat.openConn(sourceConfig);
    }

    @Override
    public void detachContext(ChannelHandlerContext context) {
        boolean wasLast = group.isEmpty();
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
        buffered = new BufferedPacketSource(sourceFactory.newSource(context, sourceConfig), packetListener, limits);
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

    private void send(BufferedPacketSource.BufferedMediaPacket buffered) {
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
//            System.err.println("write!");
            group.writeAndFlush(interleaved, ChannelMatchers.all(), true);
            stat.incByteCount(sourceConfig, payload.readableBytes() * channels);
        } else {
            stat.incLateCount(sourceConfig);
            delayedFrame = buffered;
        }
    }

    @ChannelHandler.Sharable
    private final class GroupWritabilityMonitor extends ChannelInboundHandlerAdapter {
        private Set<ChannelHandlerContext> notWritable = new HashSet<>();

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) {
            if (!ctx.channel().isWritable()) {
                incNotWritable(ctx);
            }
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) {
            decNotWritable(ctx);
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) {
            if (ctx.channel().isWritable()) {
                decNotWritable(ctx);
            } else {
                incNotWritable(ctx);
            }
        }

        private void incNotWritable(ChannelHandlerContext ctx) {
            if (notWritable.isEmpty() & notWritable.add(ctx))
                onGroupNotWritable();
//                System.err.println(notWritable.size());
            }

        private void decNotWritable(ChannelHandlerContext ctx) {
            if (notWritable.remove(ctx) & notWritable.isEmpty()) {
                onGroupWritable();
            }
        }

        public boolean isWritable() {
            return notWritable.isEmpty();
        }
    }

    private final class PacketListener implements BufferedPacketSource.BufferedMediaPacketListener {
        @Override
        public void next(BufferedPacketSource.BufferedMediaPacket bufferedMediaPacket) {
            executor.execute(() -> send(bufferedMediaPacket));
        }

        @Override
        public void end() {
            executor.execute(SyncStrategy.this::stopPlaying);
        }
    }
}
