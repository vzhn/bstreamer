package me.vzhilin.mediaserver.server;

import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import me.vzhilin.mediaserver.media.MediaStream;
import me.vzhilin.mediaserver.media.Packet;

import java.util.List;

public class RtspServerInitializer extends ChannelInitializer<SocketChannel> {
    private final List<Packet> packets;
//    private final ChannelGroup channels;

    public RtspServerInitializer() {
        packets = MediaStream.readAllPackets();
    }

    @Override
    public void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new RtpPacketEncoder());
        pipeline.addLast("http_request", new HttpRequestDecoder());
        pipeline.addLast("http_aggregator", new HttpObjectAggregator(1 * 1024));
        pipeline.addLast("http_response", new HttpResponseEncoder());
        pipeline.addLast(new RtspServerHandler(packets));
    }

}