package me.vzhilin.bstreamer.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class RtspServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    public void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new InterleavedFrameEncoder());
        pipeline.addLast("http_request", new HttpRequestDecoder());
        pipeline.addLast("http_aggregator", new HttpObjectAggregator(1024));
        pipeline.addLast("http_response", new HttpResponseEncoder());
        pipeline.addLast(new RtspServerHandler());
    }
}