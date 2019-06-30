package me.vzhilin.mediaserver.server;

import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import me.vzhilin.mediaserver.media.SourceFactoryRegistry;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactoryRegistry;

public class RtspServerInitializer extends ChannelInitializer<SocketChannel> {
    private final StreamingStrategyFactoryRegistry registry;
    private final SourceFactoryRegistry sourceFactoryRegistry;
//    private final ChannelGroup channels;

    public RtspServerInitializer(StreamingStrategyFactoryRegistry registry, SourceFactoryRegistry sourceFactoryRegistry) {
        this.registry = registry;
        this.sourceFactoryRegistry = sourceFactoryRegistry;
    }

    @Override
    public void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
//        pipeline.addLast(new RtpPacketEncoder());
        pipeline.addLast(new InterleavedFrameEncoder());
        pipeline.addLast("http_request", new HttpRequestDecoder());
        pipeline.addLast("http_aggregator", new HttpObjectAggregator(1024));
        pipeline.addLast("http_response", new HttpResponseEncoder());
        pipeline.addLast(new RtspServerHandler(registry, sourceFactoryRegistry));
    }

}