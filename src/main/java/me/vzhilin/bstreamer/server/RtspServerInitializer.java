package me.vzhilin.bstreamer.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;

public class RtspServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    public void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new InterleavedFrameEncoder());
        pipeline.addLast("http_request", new RtspDecoder());
        pipeline.addLast("http_aggregator", new HttpObjectAggregator(1024));
        pipeline.addLast("http_response", new RtspEncoder());
        pipeline.addLast(new RtspServerHandler());
    }
}