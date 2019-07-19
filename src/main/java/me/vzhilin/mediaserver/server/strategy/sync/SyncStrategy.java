package me.vzhilin.mediaserver.server.strategy.sync;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatchers;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import me.vzhilin.mediaserver.InterleavedFrame;
import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.media.CommonSourceAttributes;
import me.vzhilin.mediaserver.media.MediaPacketSource;
import me.vzhilin.mediaserver.media.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.media.file.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.server.ServerContext;
import me.vzhilin.mediaserver.server.stat.GroupStatistics;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;
import me.vzhilin.mediaserver.util.BufferedPacketSource;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public final class SyncStrategy implements StreamingStrategy {
    private final static Logger LOG = Logger.getLogger(SyncStrategy.class);

    /** executor */
    private final ScheduledExecutorService scheduledExecutor;
    private final ChannelGroup group;
    private final PropertyMap sourceConfig;
    private final MediaPacketSourceFactory sourceFactory;
    private final ServerStatistics stat;
    private final PacketListener packetListener;
    private final ServerContext context;
    private final ExecutorService executor;
    private final BufferedPacketSource.BufferingLimits limits;

    private BufferedPacketSource buffered;

    public SyncStrategy(ServerContext context, ExecutorService executor, PropertyMap sourceConfig) {
        String sourceName = sourceConfig.getValue(CommonSourceAttributes.NAME);
        this.executor = executor;
        this.context = context;
        this.sourceConfig = sourceConfig;
        this.sourceFactory = context.getSourceFactory(sourceName);
        this.scheduledExecutor = context.getScheduledExecutor();
        this.group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        this.stat = context.getStat();
        packetListener = new PacketListener();

        PropertyMap props = context.getConfig().getStrategyConfig("sync");
        int sizeLimit = props.getInt(SyncStrategyAttributes.LIMIT_SIZE);
        int packetLimit = props.getInt(SyncStrategyAttributes.LIMIT_PACKETS);
        int timeLimit = props.getInt(SyncStrategyAttributes.LIMIT_TIME);
        limits = new BufferedPacketSource.BufferingLimits(sizeLimit, packetLimit, timeLimit);
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

        statOnAttach(wasFirst);
    }

    @Override
    public void detachContext(ChannelHandlerContext context) {
        boolean wasLast = group.remove(context.channel()) && group.isEmpty();
        if (wasLast) {
            stopPlaying();
        }

        statOnDetach(wasLast);
    }

    @Override
    public MediaPacketSourceDescription describe() {
        MediaPacketSource src = sourceFactory.newSource(context, sourceConfig);
        MediaPacketSourceDescription desc = src.getDesc();
        try {
            src.close();
        } catch (IOException e) {
            LOG.error(e, e);
        }
        return desc;
    }

    private void startPlaying() {
        stat.addGroupStatistics(sourceConfig);
        buffered = new BufferedPacketSource(sourceFactory.newSource(context, sourceConfig), packetListener, limits);
        buffered.start();
    }

    private void stopPlaying() {
        try {
            buffered.stop();
        } catch (IOException e) {
            LOG.error(e, e);
        }
        group.close();
    }

    private void statOnAttach(boolean wasFirst) {
        GroupStatistics groupStat;
        if (wasFirst) {
            groupStat = stat.addGroupStatistics(sourceConfig);
        } else {
            groupStat = stat.getGroupStatistics(sourceConfig);
        }

        groupStat.incClientCount();
    }

    private void statOnDetach(boolean wasLast) {
        GroupStatistics groupStat = stat.getGroupStatistics(sourceConfig);
        groupStat.decClientCount();
        if (wasLast) {
            stat.removeGroupStatistics(sourceConfig);
        }
    }

    private void send(InterleavedFrame frame) {
        final int connectedClients = group.size();
        ByteBuf payload = frame.getPayload();
        if (connectedClients == 0) {
            payload.release();
        } else
        if (connectedClients > 1) {
            payload.retain(connectedClients - 1);
        }
        group.writeAndFlush(frame, ChannelMatchers.all(), true);
        stat.onSend(1, (long) payload.readableBytes() * connectedClients);
    }

    private boolean isChannelsWritable() {
        int[] notWritable = new int[1];
        group.forEach(channel -> {
            if (!channel.isWritable()) {
                ++notWritable[0];
            }
        });

        return notWritable[0] == 0;
    }

    private final class PacketListener implements BufferedPacketSource.BufferedMediaPacketListener {
        @Override
        public void next(BufferedPacketSource.BufferedMediaPacket bufferedMediaPacket) {
            executor.execute(() -> send(bufferedMediaPacket.drain()));
        }

        @Override
        public void end() {
            executor.execute(SyncStrategy.this::stopPlaying);
        }
    }
}
