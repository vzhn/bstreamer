package me.vzhilin.mediaserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

import java.util.List;

public class MediaPacketEncoder {
    private final int MTU = 1500;
    private int seqNo = 0;

    public void writeFuA(List<InterleavedFrame> rtpPackets, boolean isKey, long pts, ByteBuf payload) {
        int sz = payload.readableBytes();
        // FU-A
        // 18 =  4 (interleaved header) +
        //      12 (RTP header)
        //       1 (FU header)
        //       1 (FU indicator)
//        ByteBuf header = PooledByteBufAllocator.DEFAULT.buffer();

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
            ByteBuf header = PooledByteBufAllocator.DEFAULT.buffer(dataLen + 12 + 2 + 4);

            writeInterleavedHeader(header, dataLen + 12 + 2);
            writeRtpHeader(header, isKey, pts);

            header.writeByte(fuIndicator);
            header.writeByte(fuHeader);
            header.writeBytes(payload, dataLen);

            offset += dataLen;
            ++seqNo;

            rtpPackets.add(new InterleavedFrame(header));
        }
    }

    private void writeNalu(List<InterleavedFrame> rtpPackets, boolean isKey, long pts, ByteBuf payload) {

        // interleaved header
        ByteBuf bb = PooledByteBufAllocator.DEFAULT.buffer(payload.readableBytes() + 12 + 4);
        writeInterleavedHeader(bb, payload.readableBytes() + 12);

        // RTP header
        writeRtpHeader(bb, isKey, pts);
        bb.writeBytes(payload);
        rtpPackets.add(new InterleavedFrame(bb));
    }

    private void writeInterleavedHeader(ByteBuf header, int dataLen) {
        // interleaved header
        header.writeByte('$');
        header.writeByte(0);
        header.writeShort(dataLen);
    }

    private void writeRtpHeader(ByteBuf header, boolean isKey, long pts) {
        final int version = 2;
        final int payloadType = 98;
        header.writeByte((version & 0x03) << 6);
        header.writeByte(((isKey ? 1 : 0) << 7) | payloadType);
        header.writeShort(seqNo++);
        header.writeInt((int) (pts * 90));
        header.writeInt(0);
    }

    public void encode(List<InterleavedFrame> rtpPackets, boolean isKey, long pts, long dts, ByteBuf payload) {
        int sz = payload.readableBytes();
        if (sz + 16 > MTU) {
            writeFuA(rtpPackets, isKey, pts, payload);
        } else {
            writeNalu(rtpPackets, isKey, pts, payload);
        }
    }
}
