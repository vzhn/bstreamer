package me.vzhilin.mediaserver.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspVersions;
import io.netty.util.AttributeKey;

public class ClientHandler extends SimpleChannelInboundHandler<HttpObject> {
    private ConnectionStatistics statistics;

    private enum State {
        SETUP,
        PLAY,
    }

    public ClientHandler() {
    }

    private State state;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        statistics =
            ctx.channel().attr(AttributeKey.<ConnectionStatistics>valueOf("stat")).get();

        state = State.SETUP;

        HttpRequest request =
            new DefaultFullHttpRequest(RtspVersions.RTSP_1_0,
                RtspMethods.SETUP, "rtsp://localhost:5000/simpsons_video.mkv/TrackID=0");

        request.headers().set(RtspHeaderNames.CSEQ, 1);
        request.headers().set(RtspHeaderNames.TRANSPORT, "RTP/AVP/TCP;unicast;interleaved=0-1");
        ctx.writeAndFlush(request);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;

            switch (state) {
                case SETUP:
                    String cseq = response.headers().get(RtspHeaderNames.CSEQ);
                    String sessionid = response.headers().get(RtspHeaderNames.SESSION);

                    HttpRequest playRequest = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0,
                            RtspMethods.PLAY, "rtsp://localhost:5000/simpsons_video.mkv");
                    playRequest.headers().set(RtspHeaderNames.CSEQ, cseq);
                    playRequest.headers().set(RtspHeaderNames.SESSION, sessionid);
                    ctx.writeAndFlush(playRequest);

                    state = State.PLAY;
                    break;

                case PLAY:
                    ctx.channel().pipeline().addLast(new SimpleChannelInboundHandler<InterleavedPacket>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, InterleavedPacket msg)  {
                            ByteBuf payload = msg.getPayload();
                            statistics.onRead(payload.readableBytes());
                            payload.release();
                        }
                    });
                    break;
            }
        }

    }
}
