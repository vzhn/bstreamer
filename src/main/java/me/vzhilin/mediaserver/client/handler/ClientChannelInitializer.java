package me.vzhilin.mediaserver.client.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.rtsp.RtspEncoder;
import me.vzhilin.mediaserver.client.Client;
import me.vzhilin.mediaserver.client.RtspInterleavedDecoder;
import me.vzhilin.mediaserver.client.TotalStatistics;
import me.vzhilin.mediaserver.client.rtsp.NettyRtspChannelHandler;

import java.net.URI;

public final class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final StatisticHandler statisticHandler;

    public ClientChannelInitializer(TotalStatistics ss) {
        this.statisticHandler = new StatisticHandler(ss);
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        URI uri = ch.attr(Client.URL).get();
        ChannelPipeline pipeline = ch.pipeline();
        RtspInterleavedDecoder rtspInterleavedDecoder =
                new RtspInterleavedDecoder(1024, 1024, 64 * 1024);
        rtspInterleavedDecoder.setCumulator(RtspInterleavedDecoder.COMPOSITE_CUMULATOR);
        pipeline.addLast(rtspInterleavedDecoder);
        pipeline.addLast(new RtspEncoder());
        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
        pipeline.addLast(new NettyRtspChannelHandler(new ClientConnectionHandler(uri)));
        pipeline.addLast(statisticHandler);
    }
}
