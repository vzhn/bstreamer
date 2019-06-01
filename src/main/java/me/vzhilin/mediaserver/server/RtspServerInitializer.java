package me.vzhilin.mediaserver.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import me.vzhilin.mediaserver.InterleavedFrame;

public class RtspServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new MessageToByteEncoder<InterleavedFrame>() {
            @Override
            protected void encode(ChannelHandlerContext ctx, InterleavedFrame msg, ByteBuf out) {
                out.writeBytes(msg.getPayload());
            }
        });
        pipeline.addLast(new HttpRequestDecoder());
        pipeline.addLast(new HttpObjectAggregator(16 * 1024));
        pipeline.addLast(new HttpResponseEncoder());
        pipeline.addLast(new RtspServerHandler());
    }
}