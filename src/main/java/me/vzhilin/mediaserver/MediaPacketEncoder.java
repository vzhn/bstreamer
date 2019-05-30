package me.vzhilin.mediaserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import me.vzhilin.mediaserver.media.MediaPacket;

public class MediaPacketEncoder {
    private final int MTU = 1500;
    private int seqNo = 0;

    public DataChunk writeFuA(MediaPacket next, int sz) {
        // FU-A
        // 18 =  4 (interleaved header) +
        //      12 (RTP header)
        //       1 (FU header)
        //       1 (FU indicator)
        ByteBuf header = PooledByteBufAllocator.DEFAULT.buffer();

        int numberOfPackets = (sz - 2) / (MTU - 18) + 1;

        byte firstByte = next.getPayload().readByte();
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

            writeInterleavedHeader(header, dataLen + 12 + 2);
            writeRtpHeader(header, next);

            header.writeByte(fuIndicator);
            header.writeByte(fuHeader);
            header.writeBytes(next.getPayload(), dataLen);

            offset += dataLen;
            ++seqNo;
        }

        return new DataChunk(header);
    }

    private DataChunk writeNalu(MediaPacket next, int sz) {
        ByteBuf header = PooledByteBufAllocator.DEFAULT.buffer();
        // interleaved header
        writeInterleavedHeader(header, sz + 12);

        // RTP header
        writeRtpHeader(header, next);
        header.writeBytes(next.getPayload());
        return new DataChunk(header);
    }

    private void writeInterleavedHeader(ByteBuf header, int dataLen) {
        // interleaved header
        header.writeByte('$');
        header.writeByte(0);
        header.writeShort(dataLen);
    }

    private void writeRtpHeader(ByteBuf header, MediaPacket next) {
        final int version = 2;
        final int payloadType = 98;
        header.writeByte((version & 0x03) << 6);
        header.writeByte(((next.isKey() ? 1 : 0) << 7) | payloadType);
        header.writeShort(seqNo++);
        header.writeInt((int) (next.getPts() * 90));
        header.writeInt(0);
    }

    public DataChunk encode(MediaPacket packet) {
        int sz = packet.size();
        if (sz + 16 > MTU) {
            return writeFuA(packet, sz);
        } else {
            return writeNalu(packet, sz);
        }
    }
}
