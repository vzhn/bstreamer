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
import me.vzhilin.mediaserver.media.FileMediaPacketSource;
import me.vzhilin.mediaserver.media.MediaPacket;
import me.vzhilin.mediaserver.media.MediaPacketSource;
import me.vzhilin.mediaserver.media.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.server.RtpEncoder;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;
import org.ffmpeg.avutil.AVRational;
import org.ffmpeg.avutil.AVUtil;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SyncStrategy implements StreamingStrategy {
    public static final String BASE_FOLDR = "/home/vzhilin/misc/video_samples/";

    /** mkv file */
    private final String fileName;

    /** executor */
    private final ScheduledExecutorService scheduledExecutor;
    private final ChannelGroup group;

    private ScheduledFuture<?> streamingFuture;
    private MediaPacketSource source;
    private Runnable command;

    public SyncStrategy(String fileName, ScheduledExecutorService scheduledExecutor) {
        this.fileName = fileName;
        this.scheduledExecutor = scheduledExecutor;
        this.group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    }

    @Override
    public void attachContext(ChannelHandlerContext ctx) {
        boolean wasFirst = group.isEmpty();
        group.add(ctx.channel());
        ctx.channel().closeFuture().addListener((ChannelFutureListener) future -> detachContext(ctx));

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

        boolean wasLast = group.isEmpty();
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
        long dts = firstPkt.getDts();
        if (dts == AVUtil.AV_NOPTS_VALUE) {
            dts = 0;
        }

        long firstDts = dts;
        long timeStarted = System.currentTimeMillis();

        command = new Runnable() {
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

                boolean stopped = false;
                long delta = 0;
                if (notWritable > 0) {
                    System.err.println("overflow! " + notWritable + " " + adjust);
                    adjust += d;
                    delta = d;
                } else {
                    ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
                    int sz = 0;
                    while (sz < 256 * 1024) {
                        if (source.hasNext()) {
                            MediaPacket pkt = source.next();
                            sz += pkt.getPayload().readableBytes();
                            encoder.encode(buffer, pkt, rtpSeqNo++, pkt.getDts() * 90);
                            delta = (pkt.getPts() - firstDts) - (now - timeStarted) + adjust;
                            if (delta > 0) {
                                break;
                            }
                        } else {
                            stopPlaying();
                            stopped = true;
                            break;
                        }
                    }

                    if (group.size() >= 1) {
                        buffer.retain(group.size());
                        group.writeAndFlush(new InterleavedFrame(buffer), ChannelMatchers.all(), true);
                    }

                    buffer.release();
                }

                if (!stopped) {
                    streamingFuture = scheduledExecutor.schedule(command, delta, TimeUnit.MILLISECONDS);
                }
            }
        };
        streamingFuture = scheduledExecutor.schedule(command, delayNanos, TimeUnit.NANOSECONDS);
    }

    private void stopPlaying() {
        streamingFuture.cancel(true);
        group.close();
        try {
            source.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
