package me.vzhilin.bstreamer.client.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import me.vzhilin.bstreamer.client.ConnectionStatistics;
import me.vzhilin.bstreamer.client.InterleavedPacket;

@ChannelHandler.Sharable
public final class StatisticHandler extends SimpleChannelInboundHandler<InterleavedPacket> {
    private final ConnectionStatistics ss;

    public StatisticHandler(ConnectionStatistics ss) {
        super(true);
        this.ss = ss;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, InterleavedPacket msg) {
        ss.onRead(msg.getPayload().readableBytes());
        msg.getPayload().release();
    }
}
