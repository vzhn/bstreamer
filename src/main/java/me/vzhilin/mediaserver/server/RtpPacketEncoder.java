package me.vzhilin.mediaserver.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import me.vzhilin.mediaserver.media.MediaPacket;
import me.vzhilin.mediaserver.media.Packet;
import me.vzhilin.mediaserver.media.RtpPacket;

class RtpPacketEncoder extends ChannelOutboundHandlerAdapter {
    private final int MTU = 64000;
    private int seqNo = 0;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof RtpPacket) {
            encode(ctx, (RtpPacket) msg, promise);
            ctx.flush();
        } else {
            super.write(ctx, msg, promise);
        }
    }

    public void writeFuA(ChannelHandlerContext ctx, RtpPacket rtp, ChannelPromise promise) {
        MediaPacket pkt = rtp.getPkt();
        ByteBuf payload = pkt.getPayload().duplicate();

        int sz = payload.readableBytes();
        // FU-A
        // 18 =  4 (interleaved header) +
        //      12 (RTP header)
        //       1 (FU header)
        //       1 (FU indicator)
        int numberOfPackets = (sz - 2) / (MTU - 18) + 1;

        byte firstByte = payload.readByte();
        int fuIndicator = firstByte & 0xff;
        fuIndicator &= 0b11100000;
        fuIndicator += 28;

        int fuHeader = firstByte & 0xff;
        int offset = 1;
        for (int i = 0; i < numberOfPackets; ++i) {
            fuHeader &= 0b00011111;
            boolean s = i == 0;
            boolean e = i == numberOfPackets - 1;
            byte r = 0;

            if (s) {
                fuHeader |= (1 << 7);
            } else if (e) {
                fuHeader |= (1 << 6);
            }

            fuHeader |= (r & 1) << 5;

            int dataLen = Math.min(MTU - 18, sz - offset);
            ByteBuf header = PooledByteBufAllocator.DEFAULT.buffer(12 + 2 + 4, 12 + 2 + 4);

            writeInterleavedHeader(header, dataLen + 12 + 2);
            writeRtpHeader(header, pkt.isKey(), rtp.getRtpSeqNo(), rtp.getRtpTimestamp());

            header.writeByte(fuIndicator);
            header.writeByte(fuHeader);
            offset += dataLen;

            ctx.write(header, promise);
            ctx.write(payload.readRetainedSlice(dataLen), promise);
        }
    }

    private void writeNalu(ChannelHandlerContext ctx, RtpPacket rtp, ChannelPromise promise) {
        MediaPacket pkt = rtp.getPkt();
        ByteBuf payload = pkt.getPayload().retainedDuplicate();

        // interleaved header
        ByteBuf bb = PooledByteBufAllocator.DEFAULT.buffer(12 + 4, 12 + 4);
        writeInterleavedHeader(bb, payload.readableBytes() + 12);

        // RTP header
        writeRtpHeader(bb, pkt.isKey(), rtp.getRtpSeqNo(), rtp.getRtpTimestamp());
        ctx.write(bb, promise);
        ctx.write(payload, promise);
    }

    private void writeInterleavedHeader(ByteBuf header, int dataLen) {
        // interleaved header
        header.writeByte('$');
        header.writeByte(0);
        header.writeShort(dataLen);
    }

    private void writeRtpHeader(ByteBuf header, boolean isKey, long seqNo, long pts) {
        final int version = 2;
        final int payloadType = 98;
        header.writeByte((version & 0x03) << 6);
        header.writeByte(((isKey ? 1 : 0) << 7) | payloadType);
        header.writeShort((int) seqNo);
        header.writeInt((int) (pts));
        header.writeInt(0);
    }

    public void encode(ChannelHandlerContext ctx, RtpPacket rtp, ChannelPromise promise) {
        MediaPacket pkt = rtp.getPkt();
        int sz = pkt.getPayload().readableBytes();
        if (sz + 16 > MTU) {
            writeFuA(ctx, rtp, promise);
        } else {
            writeNalu(ctx, rtp, promise);
        }
    }
}
