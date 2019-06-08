package me.vzhilin.mediaserver.client;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspVersions;

public class ClientHandler extends SimpleChannelInboundHandler<HttpObject> {
    private enum State {
        SETUP,
        PLAY,
    }

    private State state;
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        state = State.SETUP;

        HttpRequest request =
            new DefaultFullHttpRequest(RtspVersions.RTSP_1_0,
                RtspMethods.SETUP, "rtsp://localhost:9995/simpsons_video.mkv/TrackID=0");

        request.headers().set(RtspHeaderNames.CSEQ, 1);
        request.headers().set(RtspHeaderNames.TRANSPORT, "RTP/AVP/TCP;unicast;interleaved=0-1");
        ctx.writeAndFlush(request);


    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        System.err.println(msg);

        if (msg instanceof DefaultHttpContent) {
            System.err.println(ByteBufUtil.prettyHexDump(((DefaultHttpContent) msg).content()));
        }


        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;

            switch (state) {
                case SETUP:
                    String cseq = response.headers().get(RtspHeaderNames.CSEQ);
                    String sessionid = response.headers().get(RtspHeaderNames.SESSION);

                    HttpRequest playRequest = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0,
                            RtspMethods.PLAY, "rtsp://localhost:9995/simpsons_video.mkv");
                    playRequest.headers().set(RtspHeaderNames.CSEQ, cseq);
                    playRequest.headers().set(RtspHeaderNames.SESSION, sessionid);
                    ctx.writeAndFlush(playRequest);

                    state = State.PLAY;
                    break;

                case PLAY:
                    ctx.channel().pipeline().addLast(new SimpleChannelInboundHandler<InterleavedPacket>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, InterleavedPacket msg)  {
                            System.err.println("packet!");
                        }
                    });
//                    ctx.channel().pipeline().remove(this);
                    break;
            }
        }

    }
}
