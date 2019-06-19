package me.vzhilin.mediaserver.server.strategy.sync;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.group.ChannelMatchers;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import me.vzhilin.mediaserver.InterleavedFrame;
import me.vzhilin.mediaserver.media.*;
import me.vzhilin.mediaserver.server.RtpEncoder;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;
import org.ffmpeg.avutil.AVRational;
import org.ffmpeg.avutil.AVUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SyncStrategy implements StreamingStrategy {
    public static final String BASE_FOLDR = "/home/vzhilin/misc/video_samples/";

    /** mkv file */
    private final String fileName;

    /** client connections */
//    private final List<ChannelHandlerContext> contexts = new ArrayList<>();

    /** executor */
    private final ScheduledExecutorService scheduledExecutor;
    private final ChannelGroup group;

    private ScheduledFuture<?> streamingFuture;
    private int contextCount = 0;
    private MediaPacketSource source;

    public SyncStrategy(String fileName, ScheduledExecutorService scheduledExecutor) {
        this.fileName = fileName;
        this.scheduledExecutor = scheduledExecutor;
        this.group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    }

    @Override
    public void attachContext(ChannelHandlerContext ctx) {
        boolean wasFirst = contextCount++ == 0;
        group.add(ctx.channel());
        ctx.channel().closeFuture().addListener((ChannelFutureListener) future -> detachContext(ctx));


//        contexts.add(ctx);

        if (wasFirst) {
            try {
                startPlaying();
            } catch (IOException ex) {
                ctx.fireExceptionCaught(ex);
            }
        }
    }



    @Override
    public void detachContext(ChannelHandlerContext context) {
        group.remove(context.channel());

        boolean wasLast = contextCount-- == 0;
        if (wasLast) {
            stopPlaying();
        }
    }

    @Override
    public MediaPacketSourceDescription describe() {
        File file = new File(BASE_FOLDR + fileName);
        try (MediaPacketSource source = new FileMediaPacketSource(file)){
            return source.getDesc();
        } catch (IOException e) {
            return null;
        }
    }

    private void startPlaying() throws IOException {
        File file = new File(BASE_FOLDR + fileName);
        source = new FileMediaPacketSource(file);
        AVRational frameRate = source.getDesc().getAvgFrameRate();
        long delayNanos = (long) (1e9 / ((float) frameRate.num() / frameRate.den()));

        delayNanos *= 1;

        MediaPacket firstPkt = source.next();
        long pts = firstPkt.getPts();
        if (pts == AVUtil.AV_NOPTS_VALUE) {
            pts = 0;
        }

        long firstPts = pts;
        long timeStarted = System.currentTimeMillis();

        streamingFuture = scheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            private final RtpEncoder encoder = new RtpEncoder();
            private long rtpSeqNo = 0;
            private int notWritable;
            private long prev = System.currentTimeMillis();
            private long adjust = 0;
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long d = now - prev;
                prev = now;

                notWritable = 0;
                group.forEach(channel -> {
                    if (!channel.isWritable()) {
                        ++notWritable;
                    }
                });

                if (notWritable > 0) {
                    System.err.println("overflow! " + notWritable);
                    adjust += d;
                } else {
                    ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
                    int sz = 0;
                    while (sz < 512 * 1024) {
                        if (source.hasNext()) {
                            MediaPacket pkt = source.next();
                            sz += pkt.getPayload().readableBytes();
                            encoder.encode(buffer, pkt, rtpSeqNo++, pkt.getPts() * 90);
                            long delta = (pkt.getPts() - firstPts) - (now - timeStarted) + adjust;
                            if (delta >= 100) {
                                break;
                            }
                        } else {
                            stopPlaying();
                            break;
                        }
                    }

                    if (group.size() >= 1) {
                        buffer.retain(group.size());
                        group.writeAndFlush(new InterleavedFrame(buffer), ChannelMatchers.all(), true);
                    }

                    buffer.release();
                }


            }
        }, 0, delayNanos, TimeUnit.NANOSECONDS);
    }

    private void stopPlaying() {
        streamingFuture.cancel(true);
        group.close();
//        for (ChannelHandlerContext ctx: contexts) {
//            ctx.close();
//        }
        try {
            source.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class MyChannelMatcher implements ChannelMatcher {
        private final long[] overflow;
        private final ByteBuf buffer;

        public MyChannelMatcher(long[] overflow, ByteBuf buffer) {
            this.overflow = overflow;
            this.buffer = buffer;
        }

        @Override
        public boolean matches(Channel channel) {
            boolean writable = channel.isWritable();
            if (!writable) {
                ++overflow[0];
                buffer.release();
            }
            return writable;
        }
    }
}
