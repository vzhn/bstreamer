package me.vzhilin.bstreamer.client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import me.vzhilin.bstreamer.client.ConnectionStatistics;
import me.vzhilin.bstreamer.client.InterleavedPacket;

public final class StatisticHandler extends SimpleChannelInboundHandler<InterleavedPacket> {
    private final ConnectionStatistics ss;

    private long prevSeqNumber = Long.MIN_VALUE;

    public StatisticHandler(ConnectionStatistics ss) {
        super(true);
        this.ss = ss;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, InterleavedPacket msg) {
        ByteBuf pl = msg.getPayload();
        ss.onRead(pl.readableBytes());

        int seqNumber = pl.getUnsignedShort(2);
        pl.release();
        if (prevSeqNumber != Long.MIN_VALUE) {
            if (((seqNumber - prevSeqNumber) & 0xffff) != 1) {
                ss.onLostPacket();
            }
        }
        prevSeqNumber = seqNumber;
    }
}
