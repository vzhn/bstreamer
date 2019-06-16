package me.vzhilin.mediaserver.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspVersions;
import me.vzhilin.mediaserver.InterleavedFrame;
import me.vzhilin.mediaserver.media.Packet;
import me.vzhilin.mediaserver.media.RtpPacket;
import me.vzhilin.mediaserver.util.AVCCExtradataParser;
import me.vzhilin.mediaserver.util.RtspUriParser;
import org.bridj.Pointer;
import org.ffmpeg.avcodec.AVCodecParameters;
import org.ffmpeg.avformat.AVFormatContext;
import org.ffmpeg.avformat.AVInputFormat;
import org.ffmpeg.avformat.AVStream;
import org.ffmpeg.avutil.AVDictionary;

import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.ffmpeg.avformat.AvformatLibrary.*;

public class RtspServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final List<Packet> packets;
    private Strategy strategy;

    public RtspServerHandler(List<Packet> packets) {
        this.packets = packets;
    }

    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        FullHttpRequest request = (FullHttpRequest) msg;

        HttpMethod method = request.method();
        HttpResponse response;
        switch (method.name()) {
            case "OPTIONS":
                response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.OK);
                response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
                response.headers().set(RtspHeaderNames.PUBLIC, "DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE");
                ctx.writeAndFlush(response);
                break;
            case "DESCRIBE":
                RtspUriParser uri = new RtspUriParser(msg.uri());
                response = description(uri);
                response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
                ctx.writeAndFlush(response);
                break;
            case "SETUP":
                response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.OK);
                response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
                response.headers().set(RtspHeaderNames.SESSION, "1234");
                response.headers().set(RtspHeaderNames.TRANSPORT, "RTP/AVP/TCP;unicast;interleaved=0-1");
                ctx.writeAndFlush(response);
                break;
            case "PLAY":
                response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.OK);
                response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
                response.headers().set(RtspHeaderNames.SESSION, request.headers().get(RtspHeaderNames.SESSION));
                ctx.writeAndFlush(response);

                strategy.play();
                break;
            default:
                response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.BAD_REQUEST);
                response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
                ctx.writeAndFlush(response);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);

        if (evt instanceof TickEvent) {
            sendMaximum(ctx, ctx.channel());
        }
    }

    private void send(ChannelHandlerContext ctx) {

    }

    private void sendMaximum(ChannelHandlerContext ctx, Channel channel) {
//        InterleavedFrame next;
//        while (channel.isWritable()) {
//            if (!stream.hasNext()) {
//                stream = packets.iterator();
//            }
//            next = stream.next();
//            ctx.write(next, ctx.voidPromise());
//        }

//        System.err.println("write! " + new Date());
        ctx.flush();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.strategy = new Strategy(ctx, packets);
    }



    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    FullHttpResponse description(RtspUriParser uri) {
        String filename = uri.getPathItems().get(0);

        AVCCExtradataParser avccExtradata = getExtradata(filename);
        byte[] sps = avccExtradata.getSps();
        String spsBase64 = Base64.getEncoder().encodeToString(sps);
        String ppsBase64 = Base64.getEncoder().encodeToString(avccExtradata.getPps());

        String profileLevelId = String.format("%02x%02x%02x", sps[0], sps[1], sps[2]);

        String sdpMessage = "v=0\r\n" +
                "o=RTSP 50539017935697 1 IN IP4 0.0.0.0\r\n" +
                "s=1234\r\n" +
                "a=control:*\r\n" +
                "m=video 0 RTP/AVP 98\r\n" +
                "a=fmtp:98 sprop-parameter-sets=" +
                spsBase64 + "," + ppsBase64 + ";profile-level-id=" + profileLevelId
                + ";packetization-mode=1\r\n"
                + "a=rtpmap:98 H264/90000\r\n"
                + "a=control:TrackID=0\r\n";

        ByteBuf payload = ByteBufUtil.writeAscii(PooledByteBufAllocator.DEFAULT, sdpMessage);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.OK, payload);
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH, payload.readableBytes());
        response.headers().add(HttpHeaderNames.CONTENT_BASE, uri.getUri());
        response.headers().add(HttpHeaderNames.CONTENT_TYPE, "application/sdp");

        return response;
    }

    private AVCCExtradataParser getExtradata(String filename) {
        Pointer<Pointer<AVFormatContext>> pAvfmtCtx = Pointer.allocatePointer(AVFormatContext.class);
        Pointer<Byte> namePtr = Pointer.pointerToCString("/home/vzhilin/misc/video_samples/" + filename);

        Pointer<AVInputFormat> fmt = null;
        Pointer<Pointer<AVDictionary>> options = null;

        avformat_open_input(pAvfmtCtx, namePtr, fmt, options);
        Pointer<AVFormatContext> ifmtCtx = pAvfmtCtx.get();

        avformat_find_stream_info(ifmtCtx, null);
        int ns = ifmtCtx.get().nb_streams();
        Pointer<AVStream> avstream = ifmtCtx.get().streams().get(0);
        AVStream avStream = avstream.get();
        Pointer<AVCodecParameters> cp = avStream.codecpar();
        AVCodecParameters codecpar = cp.get();

        byte[] extradata = codecpar.extradata().getBytes(codecpar.extradata_size());

        avformat_close_input(pAvfmtCtx);
        namePtr.release();
        pAvfmtCtx.release();

        return new AVCCExtradataParser(extradata);
    }

    private final static class Strategy {
        private final ChannelHandlerContext ctx;
        private final List<Packet> packets;

        private Iterator<Packet> stream;
        private long timeStart;
        private long ptsStart;

        private long rtpSeqNo;

        private Packet prevPkt;

        public Strategy(ChannelHandlerContext ctx, List<Packet> packets) {
            this.ctx = ctx;
            this.packets = packets;
        }

        void play() {
            this.stream = packets.iterator();
            Packet firstFrame = stream.next();
            timeStart = System.currentTimeMillis();
            ptsStart = firstFrame.getPts();
            ctx.writeAndFlush(firstFrame, ctx.voidPromise());

            send();
        }

        void send() {
            if (!ctx.channel().isActive()) {
                return;
            }

            long now = System.currentTimeMillis();

            if (prevPkt != null) {
                long pd = (prevPkt.getPts() - ptsStart) - (now - timeStart);
                if (pd < 0) {
                    System.err.println("late! " + pd);
                    timeStart += -pd;
//                    ptsStart += -pd;
                }
            }

            long sz = 0;
            Packet next = null;
            long delay;
            do {
                if (!stream.hasNext()) {
                    stream = packets.iterator();

                    timeStart = now;
                    ptsStart = 0;
                }

                next = stream.next();
                prevPkt = next;
                long rtpTimestamp = (next.getPts() - ptsStart) * 90;
                ctx.write(new RtpPacket(next, rtpTimestamp, rtpSeqNo++), ctx.voidPromise());
                sz += next.getPayload().readableBytes();
                delay = (next.getPts() - ptsStart) - (now - timeStart);

            } while (delay < 500);

            System.err.println("delay: " + delay + " " + sz);

            ctx.executor().schedule(this::send, delay - 100, TimeUnit.MILLISECONDS);
            ctx.flush();
        }

        void stop() {

        }


        private long pktTime(long pktPts) {
            return timeStart + pktPts;
        }
    }
}
