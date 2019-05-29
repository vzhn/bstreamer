package me.vzhilin.mediaserver.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspVersions;
import me.vzhilin.mediaserver.media.MediaPacket;
import me.vzhilin.mediaserver.media.MediaStream;
import org.bridj.Pointer;
import org.ffmpeg.avcodec.AVCodecParameters;
import org.ffmpeg.avformat.AVFormatContext;
import org.ffmpeg.avformat.AVInputFormat;
import org.ffmpeg.avformat.AVStream;
import org.ffmpeg.avutil.AVDictionary;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.ffmpeg.avformat.AvformatLibrary.*;

public class RtspServerHandler extends SimpleChannelInboundHandler<Object> {
    private MediaStream stream;
    private int seqNo = 0;

    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;

            HttpMethod method = request.method();
            HttpResponse response;
            switch (method.name()) {
                case "OPTIONS":
                    response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.OK);
                    response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
                    response.headers().set(RtspHeaderNames.PUBLIC, "DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE");
                    break;
                case "DESCRIBE":
                    response = description("simpsons_video.mkv");
                    response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
                    break;
                case "SETUP":
                    response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.OK);
                    response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
                    response.headers().set(RtspHeaderNames.SESSION, "1234");
                    response.headers().set(RtspHeaderNames.TRANSPORT, "RTP/AVP/TCP;unicast;interleaved=0-1");
                    break;
                case "PLAY":
                    response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.OK);
                    response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
                    response.headers().set(RtspHeaderNames.SESSION, request.headers().get(RtspHeaderNames.SESSION));
                    break;
                default:
                    response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.BAD_REQUEST);
                    response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
            }

            ctx.writeAndFlush(response);

            if (method.name().equals("PLAY")) {
                this.stream = new MediaStream();
                ctx.executor().scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        send(ctx, stream.next());
                    }
                }, 100, 40, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void send(ChannelHandlerContext ctx, List<MediaPacket> next) {
        next.forEach(ctx::writeAndFlush);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
//        MediaPacket pkt = stream.next();
//        if (pkt != null) {
//            send(ctx, pkt);
//        }
    }

    FullHttpResponse description(String name) {
        Pointer<Pointer<AVFormatContext>> pAvfmtCtx = Pointer.allocatePointer(AVFormatContext.class);
        Pointer<Byte> namePtr = Pointer.pointerToCString("/home/vzhilin/misc/video_samples/simpsons_video.mkv");

        Pointer<AVInputFormat> fmt = (Pointer<AVInputFormat>) Pointer.NULL;
        Pointer<Pointer<AVDictionary> > options = (Pointer<Pointer<AVDictionary>>) Pointer.NULL;

        avformat_open_input(pAvfmtCtx, namePtr, fmt, options);
        Pointer<AVFormatContext> ifmtCtx = pAvfmtCtx.get();

        avformat_find_stream_info(ifmtCtx, (Pointer<Pointer<AVDictionary>>) Pointer.NULL);
        int ns = ifmtCtx.get().nb_streams();
        Pointer<AVStream> avstream = ifmtCtx.get().streams().get(0);
        AVStream avStream = avstream.get();
        Pointer<AVCodecParameters> cp = avStream.codecpar();
        AVCodecParameters codecpar = cp.get();

        byte[] extradata = codecpar.extradata().getBytes(codecpar.extradata_size());
        AVCCExtradata avccExtradata = new AVCCExtradata(extradata);
        byte[] sps = avccExtradata.getSps();
        String spsBase64 = Base64.getEncoder().encodeToString(sps);
        String ppsBase64 = Base64.getEncoder().encodeToString(avccExtradata.getPps());

        String profileLevelId = String.format("%02x%02x%02x", sps[0], sps[1], sps[2]);

        StringBuilder sdpMessage = new StringBuilder();
        sdpMessage.append(
            "v=0\r\n" +
            "o=RTSP 50539017935697 1 IN IP4 0.0.0.0\r\n"+
            "s=1234\r\n"+
            "a=control:*\r\n"+
            "m=video 0 RTP/AVP 98\r\n"+
            "a=fmtp:98 sprop-parameter-sets="
        );

        sdpMessage.append(spsBase64 + "," + ppsBase64 +  ";profile-level-id=" + profileLevelId
                + ";packetization-mode=1\r\n"
                +  "a=rtpmap:98 H264/90000\r\n"
                +  "a=control:TrackID=0\r\n");

        ByteBuf payload = ByteBufUtil.writeAscii(PooledByteBufAllocator.DEFAULT, sdpMessage.toString());
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.OK, payload);
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH, payload.readableBytes());
        response.headers().add(HttpHeaderNames.CONTENT_BASE, "rtsp://localhost:5000/simpsons_video.mkv");
        response.headers().add(HttpHeaderNames.CONTENT_TYPE, "application/sdp");

        return response;
    }

    private final static class AVCCExtradata {
        private final byte[] sps;
        private final byte[] pps;

        public AVCCExtradata(byte[] extradata) {
            ByteBuf is = Unpooled.wrappedBuffer(extradata);
            int v = is.readByte();

            int profile = is.readByte();
            int compatibility = is.readByte();
            int level = is.readByte();

            int naluLengthMinusOne = (is.readByte() & 0xff) & 0b11;
            if (naluLengthMinusOne != 3) {
                throw new RuntimeException("not supported: naluLengthMinusOne != 3");
            }

            int spsNumber = is.readByte() & 0b11111;
            if (spsNumber != 1) {
                throw new RuntimeException("not supported: spsNumber != 1");
            }

            int spsLen = ((is.readByte() & 0xff) << 8) + is.readByte() & 0xff;
            sps = new byte[spsLen];
            is.readBytes(sps);

            int numPps = is.readByte() & 0xff;
            if (numPps != 1) {
                throw new RuntimeException();
            }

            int ppsLen = ((is.readByte() & 0xff) << 8) + is.readByte() & 0xff;
            pps = new byte[ppsLen];
            is.readBytes(pps);
        }

        public byte[] getSps() {
            return sps;
        }

        public byte[] getPps() {
            return pps;
        }
    }
}
