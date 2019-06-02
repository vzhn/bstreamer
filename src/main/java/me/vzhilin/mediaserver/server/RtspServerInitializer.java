package me.vzhilin.mediaserver.server;

import io.netty.channel.*;
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

public class RtspServerInitializer extends ChannelInitializer<SocketChannel> {
    private final TypeParameterMatcher matcher = TypeParameterMatcher.get(InterleavedFrame.class);
    private final Stream<InterleavedFrame> stream;

    public RtspServerInitializer() {
        stream = new Stream<InterleavedFrame>(new CircularBuffer(MediaStream.readAllPackets())) {
            private int seqNo = 0;
            @Override
            public Node<InterleavedFrame> allocNode() {
                Node<InterleavedFrame> node = super.allocNode();
                InterleavedFrame frame = node.getValue();
                frame.setSeqNo(seqNo++);
                return node;
            }
        };
    }

    @Override
    public void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
                if (matcher.match(msg)) {
                    ctx.write(((InterleavedFrame) msg).getPayload().retainedSlice(), promise);
                } else {
                    ctx.write(msg, promise);
                }
            }
        });
        pipeline.addLast("http_request", new HttpRequestDecoder());
        pipeline.addLast("http_aggregator", new HttpObjectAggregator(16 * 1024));
        pipeline.addLast("http_response", new HttpResponseEncoder());
        pipeline.addLast(new RtspServerHandler(stream));
    }
}