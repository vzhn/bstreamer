package me.vzhilin.mediaserver.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspVersions;
import me.vzhilin.mediaserver.media.*;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactory;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactoryRegistry;
import me.vzhilin.mediaserver.util.RtspUriParser;

import java.util.Base64;

public class RtspServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final StreamingStrategyFactoryRegistry registry;

    public RtspServerHandler(StreamingStrategyFactoryRegistry registry) {
        this.registry = registry;
    }

    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        HttpMethod method = request.method();
        HttpResponse response;
        switch (method.name()) {
            case "OPTIONS":
                response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.OK);
                response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
                response.headers().set(RtspHeaderNames.PUBLIC, "DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE");
                ctx.writeAndFlush(response);
                break;
            case "DESCRIBE": {
                RtspUriParser uri = new RtspUriParser(request.uri());

                if (uri.pathItems() < 2) {
                    response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.BAD_REQUEST);
                    response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
                    ctx.writeAndFlush(response);
                } else {
                    String strategyName = uri.pathItem(0);
                    String fileName = uri.pathItem(1);


                    MediaPacketSourceFactory fsf = new FileSourceFactory(fileName);
                    StreamingStrategyFactory strategyFactory = registry.get(strategyName);
                    MediaPacketSourceDescription description = strategyFactory.describe(fsf);
                    response = description(uri, description);
                    response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
                    ctx.writeAndFlush(response);
                }

                break;
            }
            case "SETUP": {
                response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.OK);
                response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
                response.headers().set(RtspHeaderNames.SESSION, "1234");
                response.headers().set(RtspHeaderNames.TRANSPORT, "RTP/AVP/TCP;unicast;interleaved=0-1");
                ctx.writeAndFlush(response);
                break;
            }
            case "PLAY": {
                RtspUriParser uri = new RtspUriParser(request.uri());
                String strategyName = uri.pathItem(0);
                String fileName = uri.pathItem(1);

                StreamingStrategyFactory strategyFactory = registry.get(strategyName);

                response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.OK);
                response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
                response.headers().set(RtspHeaderNames.SESSION, request.headers().get(RtspHeaderNames.SESSION));
                ctx.writeAndFlush(response);

                StreamingStrategy strategy = strategyFactory.getStrategy(new FileSourceFactory(fileName));
                strategy.attachContext(ctx);
                break;
            }
            default:
                response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.BAD_REQUEST);
                response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
                ctx.writeAndFlush(response);
        }
    }

    private FullHttpResponse description(RtspUriParser uri, MediaPacketSourceDescription description) {
        byte[] sps = description.getSps();
        byte[] pps = description.getPps();
        String spsBase64 = Base64.getEncoder().encodeToString(sps);
        String ppsBase64 = Base64.getEncoder().encodeToString(pps);

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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
