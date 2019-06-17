package me.vzhilin.mediaserver.server.strategy.sync;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
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

public class SyncStrategy implements StreamingStrategy {
    public static final String BASE_FOLDR = "/home/vzhilin/misc/video_samples/";

    /** mkv file */
    private final String fileName;

    /** client connections */
    private final List<ChannelHandlerContext> contexts = new ArrayList<>();

    /** executor */
    private final ScheduledExecutorService scheduledExecutor;
    private final ChannelGroup group;

    private ScheduledFuture<?> streamingFuture;

    private MediaPacketSource source;

    public SyncStrategy(String fileName, ScheduledExecutorService scheduledExecutor) {
        this.fileName = fileName;
        this.scheduledExecutor = scheduledExecutor;
        this.group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    }

    @Override
    public void attachContext(ChannelHandlerContext ctx) {
        group.add(ctx.channel());
        ctx.channel().closeFuture().addListener((ChannelFutureListener) future -> detachContext(ctx));

        boolean wasFirst = contexts.isEmpty();
        contexts.add(ctx);

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

        boolean wasLast = contexts.remove(context) && contexts.isEmpty();
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
            @Override
            public void run() {
                long now = System.currentTimeMillis();

//                int sz = encoder.estimateSize(pkt);
                ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
                int packets = 0;
                while (true) {
                    if (source.hasNext()) {
                        MediaPacket pkt = source.next();
                        ++packets;


                        encoder.encode(buffer, pkt, rtpSeqNo++, pkt.getPts() * 90);

//                        boolean success = sendRtpPkt(pkt);

                        long delta = (pkt.getPts() - firstPts) - (now - timeStarted);
                        if (delta >= 100) {
                            break;
                        }

//                        if (!success) {
//                            break;
//                        }
                    } else {
                        stopPlaying();
                        break;
                    }
                }

                long delta = System.currentTimeMillis() - now;
                if (contexts.size() > 1) {
                    buffer.retain(contexts.size() - 1);
                }

                group.writeAndFlush(new InterleavedFrame(buffer), ChannelMatchers.all(), true);
                System.err.println(delta + " " + packets + " " + buffer.readableBytes());
            }

            private boolean sendPkt(MediaPacket pkt) {
                long rtpTimestamp = pkt.getPts() * 90;
                RtpPacket rtpPkt = new RtpPacket(pkt, rtpTimestamp, rtpSeqNo++);

                boolean success = false;

                group.write(rtpPkt, ChannelMatchers.all(), true);

//                for (ChannelHandlerContext ctx: contexts) {
//                    if (ctx.channel().isWritable()) {
//                        success |= true;
//                        ctx.writeAndFlush(rtpPkt, ctx.voidPromise());
//                    }
//                }

                return success;
            }
        }, 0, delayNanos, TimeUnit.NANOSECONDS);
    }

    private void stopPlaying() {
        streamingFuture.cancel(true);
        for (ChannelHandlerContext ctx: contexts) {
            ctx.close();
        }
        try {
            source.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
