package me.vzhilin.mediaserver.server;

import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.internal.TypeParameterMatcher;
import me.vzhilin.mediaserver.InterleavedFrame;
import me.vzhilin.mediaserver.media.MediaStream;
import me.vzhilin.mediaserver.stream.Node;
import me.vzhilin.mediaserver.stream.Stream;
import me.vzhilin.mediaserver.stream.impl.CircularBuffer;

import java.util.List;

public class RtspServerInitializer extends ChannelInitializer<SocketChannel> {
    private final List<InterleavedFrame> packets;
//    private final ChannelGroup channels;

    public RtspServerInitializer() {
        packets = MediaStream.readAllPackets();
    }

    @Override
    public void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new MyChannelOutboundHandlerAdapter());
        pipeline.addLast("http_request", new HttpRequestDecoder());
        pipeline.addLast("http_aggregator", new HttpObjectAggregator(1 * 1024));
        pipeline.addLast("http_response", new HttpResponseEncoder());
        pipeline.addLast(new RtspServerHandler(packets));

//        channels.add(channel);
    }

    private final static class MyChannelOutboundHandlerAdapter extends ChannelOutboundHandlerAdapter {
        private final TypeParameterMatcher matcher = TypeParameterMatcher.get(InterleavedFrame.class);
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            if (matcher.match(msg)) {
                ctx.write(((InterleavedFrame) msg).getPayload().retainedDuplicate(), promise);
            } else {
                ctx.write(msg, promise);
            }
        }
    }
}