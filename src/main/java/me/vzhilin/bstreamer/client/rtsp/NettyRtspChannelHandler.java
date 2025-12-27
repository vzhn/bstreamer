package me.vzhilin.bstreamer.client.rtsp;
//CS_OFF:IllegalThrows

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspVersions;
import io.netty.util.CharsetUtil;
import me.vzhilin.bstreamer.client.rtsp.messages.DescribeReply;
import me.vzhilin.bstreamer.client.rtsp.messages.GetParameterReply;
import me.vzhilin.bstreamer.client.rtsp.messages.PlayReply;
import me.vzhilin.bstreamer.client.rtsp.messages.SetupReply;
import me.vzhilin.bstreamer.client.rtsp.messages.sdp.SdpMessage;
import me.vzhilin.bstreamer.client.rtsp.messages.sdp.SdpParser;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class NettyRtspChannelHandler extends SimpleChannelInboundHandler<FullHttpResponse> implements RtspConnection {
    private final RtspConnectionHandler connectionHandler;
    private ChannelHandlerContext ctx;

    private int cseq = 1;
    private final Map<Integer, ReplyHandler> cseqToReplyHandler = new HashMap<>();

    public NettyRtspChannelHandler(RtspConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        connectionHandler.onConnected(this);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connectionHandler.onDisconnected();
        for (ReplyHandler unprocessed : cseqToReplyHandler.values()) {
            unprocessed.onError();
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.channel().close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
        int replyCseq = Integer.parseInt(msg.headers().get(RtspHeaderNames.CSEQ));
        ReplyHandler handler = cseqToReplyHandler.remove(replyCseq);
        if (HttpResponseStatus.OK.equals(msg.status())) {
            handler.read(ctx, msg);
        } else {
            handler.onError();
        }
    }

    @Override
    public void describe(URI uri, final RtspCallback<DescribeReply> cb) {
        int requestCseq = cseq++;
        cseqToReplyHandler.put(requestCseq, new DescribeReplyHandler(cb));
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.DESCRIBE, uri.toString());
        request.headers().add(RtspHeaderNames.CSEQ, requestCseq);
        ctx.writeAndFlush(request);
    }

    @Override
    public void setup(URI uri, final RtspCallback<SetupReply> cb) {
        int requestCseq = cseq++;
        cseqToReplyHandler.put(requestCseq, new SetupReplyHandler(cb));

        DefaultFullHttpRequest setupRequest = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.SETUP, uri.toString());
        HttpHeaders headers = setupRequest.headers();
        headers.add(RtspHeaderNames.CSEQ, requestCseq);

        int rtpChannel = 0;
        int rtcpChannel = rtpChannel + 1;
        headers.add(RtspHeaderNames.TRANSPORT, String.format("RTP/AVP/TCP;unicast;interleaved=%d-%d", rtpChannel, rtcpChannel));
        ctx.writeAndFlush(setupRequest);
    }

    @Override
    public void getParameter(URI uri, String session, RtspCallback<GetParameterReply> cb) {
        int requestCseq = cseq++;
        cseqToReplyHandler.put(requestCseq, new GetParameterReplyHandler());

        DefaultFullHttpRequest getParameterRequest =
            new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.GET_PARAMETER, uri.toString());

        getParameterRequest.headers().add(RtspHeaderNames.CSEQ, requestCseq);
        getParameterRequest.headers().add(RtspHeaderNames.SESSION, session);
        ctx.writeAndFlush(getParameterRequest);
    }

    @Override
    public void setParameter(URI uri, String session, Map<String, String> parameters) {
        int requestCseq = cseq++;
        cseqToReplyHandler.put(requestCseq, new SetParameterReplyHandler());

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e: parameters.entrySet()) {
            String name = e.getKey();
            String value = e.getValue();
            sb.append(name).append(": ").append(value).append('\n');
        }

        String contentString = sb.toString();
        byte[] bytes = contentString.getBytes();
        ByteBuf content = PooledByteBufAllocator.DEFAULT.buffer(bytes.length);
        content.writeBytes(bytes);
        DefaultFullHttpRequest setParameterRequest =
            new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.SET_PARAMETER, uri.toString(), content);

        HttpHeaders headers = setParameterRequest.headers();
        headers.add(RtspHeaderNames.CSEQ, requestCseq);
        headers.add(RtspHeaderNames.SESSION, session);
        headers.add(RtspHeaderNames.CONTENT_LENGTH, content.readableBytes());
        headers.add(RtspHeaderNames.CONTENT_TYPE, "text/parameters");
        ctx.writeAndFlush(setParameterRequest);
    }

    @Override
    public void play(URI uri, String session, final RtspCallback<PlayReply> cb) {
        int requestCseq = cseq++;
        cseqToReplyHandler.put(requestCseq, new PlayReplyHandler(cb));
        DefaultFullHttpRequest playRequest = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.PLAY, uri + "/");
        playRequest.headers().add(RtspHeaderNames.CSEQ, requestCseq);
        playRequest.headers().add(RtspHeaderNames.SESSION, session);
        ctx.writeAndFlush(playRequest);

        ctx.executor().schedule(new KeepAliveTask(ctx.channel(), uri, session), 55, TimeUnit.SECONDS);
    }

    @Override
    public void disconnect() {
        ctx.channel().disconnect();
    }

    interface ReplyHandler {
        void read(ChannelHandlerContext ctx, FullHttpResponse msg);
        void onError();
    }

    private final static class DescribeReplyHandler implements ReplyHandler {
        private final RtspCallback<DescribeReply> cb;

        private DescribeReplyHandler(RtspCallback<DescribeReply> cb) {
            this.cb = cb;
        }

        @Override
        public void read(ChannelHandlerContext ctx, FullHttpResponse msg) {
            final String sdp = msg.content().toString(CharsetUtil.UTF_8);
            SdpMessage sdpMessage = new SdpParser().parse(sdp);
            String contentBase = msg.headers().get(RtspHeaderNames.CONTENT_BASE);
            cb.onSuccess(new DescribeReply(contentBase, sdpMessage));
        }

        @Override
        public void onError() {
            cb.onError();
        }
    }

    private final static class SetupReplyHandler implements ReplyHandler {
        private final RtspCallback<SetupReply> cb;
        private SetupReplyHandler(RtspCallback<SetupReply> cb) {
            this.cb = cb;
        }

        @Override
        public void read(ChannelHandlerContext ctx, FullHttpResponse msg) {
            String session = msg.headers().get(RtspHeaderNames.SESSION);
            if (session.contains(";")) {
                session = session.substring(0, session.indexOf(';'));
            }

            cb.onSuccess(new SetupReply(session));
        }

        @Override
        public void onError() {
            cb.onError();
        }
    }

    private static class GetParameterReplyHandler implements ReplyHandler {
        @Override
        public void read(ChannelHandlerContext ctx, FullHttpResponse msg) { }

        @Override
        public void onError() { }
    }

    private static class SetParameterReplyHandler implements ReplyHandler {
        @Override
        public void read(ChannelHandlerContext ctx, FullHttpResponse msg) { }

        @Override
        public void onError() { }
    }

    private static class PlayReplyHandler implements ReplyHandler {
        private final RtspCallback<PlayReply> cb;

        private PlayReplyHandler(RtspCallback<PlayReply> cb) {
            this.cb = cb;
        }

        @Override
        public void read(ChannelHandlerContext ctx, FullHttpResponse msg) {
            cb.onSuccess(new PlayReply());
        }

        @Override
        public void onError() {
            cb.onError();
        }
    }

    private final class KeepAliveTask implements Runnable {
        private final String session;
        private final Channel ch;
        private final URI uri;

        private final RtspCallback<GetParameterReply> emptyCallback = new RtspCallback<GetParameterReply>() {
            @Override
            public void onSuccess(GetParameterReply mesg) { }
            @Override
            public void onError() { }
        };

        private KeepAliveTask(Channel ch, URI uri, String session) {
            this.ch = ch;
            this.uri = uri;
            this.session = session;
        }

        @Override
        public void run() {
            if (ch.isActive()) {
                getParameter(uri, session, emptyCallback);
                ch.eventLoop().schedule(this, 55, TimeUnit.SECONDS);
            }
        }
    }
}
