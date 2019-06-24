package me.vzhilin.mediaserver.server.strategy.seq;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import me.vzhilin.mediaserver.media.MediaPacketSource;
import me.vzhilin.mediaserver.media.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.media.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SequencedStrategy implements StreamingStrategy {
    private final MediaPacketSourceFactory sourceFactory;
    private final ScheduledExecutorService scheduledExecutor;
    private final ServerStatistics stat;
    private MediaPacketSource src;
    private Runnable command;
    private ScheduledFuture<?> streamingFuture;

    public SequencedStrategy(MediaPacketSourceFactory sourceFactory,
                             ScheduledExecutorService scheduledExecutor,
                             ServerStatistics stat) {

        this.scheduledExecutor = scheduledExecutor;
        this.sourceFactory = sourceFactory;
        this.stat = stat;
    }

    @Override
    public void attachContext(ChannelHandlerContext ctx) {
        Channel ch = ctx.channel();
        ch.closeFuture().addListener((ChannelFutureListener) future -> detachContext(ctx));

//        ch.att

        src = sourceFactory.newSource();

        command = new Runnable() {
            @Override
            public void run() {

            }
        };
        streamingFuture = scheduledExecutor.schedule(command, 0, TimeUnit.NANOSECONDS);
    }

    @Override
    public void detachContext(ChannelHandlerContext context) {
        streamingFuture.cancel(true);
        try {
            src.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MediaPacketSourceDescription describe() {
        return src.getDesc();
    }
}
