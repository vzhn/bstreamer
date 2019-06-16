package me.vzhilin.mediaserver.server.strategy.sync;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import me.vzhilin.mediaserver.media.*;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;
import org.ffmpeg.avcodec.AVPacket;
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

    private ScheduledFuture<?> streamingFuture;

    private MediaPacketSource source;

    public SyncStrategy(String fileName, ScheduledExecutorService scheduledExecutor) {
        this.fileName = fileName;
        this.scheduledExecutor = scheduledExecutor;
    }

    @Override
    public void attachContext(ChannelHandlerContext ctx) {
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

        MediaPacket firstPkt = source.next();
        long pts = firstPkt.getPts();
        if (pts == AVUtil.AV_NOPTS_VALUE) {
            pts = 0;
        }

        long firstPts = pts;
        long timeStarted = System.currentTimeMillis();

        streamingFuture = scheduledExecutor.scheduleAtFixedRate(new Runnable() {
            private long rtpSeqNo = 0;
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                while (true) {
                    if (source.hasNext()) {
                        MediaPacket pkt = source.next();
                        sendPkt(pkt);

                        long delta = (pkt.getPts() - firstPts) - (now - timeStarted);
                        if (delta >= 0) {
                            break;
                        }
                    } else {
                        stopPlaying();
                        break;
                    }
                }

            }

            private void sendPkt(MediaPacket pkt) {
                long rtpTimestamp = pkt.getPts() * 90;
                RtpPacket rtpPkt = new RtpPacket(pkt, rtpTimestamp, rtpSeqNo++);

                for (ChannelHandlerContext ctx: contexts) {
                    ctx.writeAndFlush(rtpPkt, ctx.voidPromise());
                }
            }
        }, 0, delayNanos, TimeUnit.NANOSECONDS);
    }

    private void stopPlaying() {
        streamingFuture.cancel(true);
        for (ChannelHandlerContext ctx: contexts) {
            ctx.close(ctx.voidPromise());
        }
        try {
            source.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
