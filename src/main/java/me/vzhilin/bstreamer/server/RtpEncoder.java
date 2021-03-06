package me.vzhilin.bstreamer.server;

import io.netty.buffer.ByteBuf;
import me.vzhilin.bstreamer.server.streaming.file.MediaPacket;

public class RtpEncoder {
    private final int maxRtpSize;
    private long seqNo = 0;

    public RtpEncoder(int maxRtpSize) {
        this.maxRtpSize = maxRtpSize;
        if (this.maxRtpSize > 65536 || this.maxRtpSize <= 18) {
            throw new RuntimeException("incorrect maxRtpSize");
        }
    }

    public void encode(ByteBuf buffer, MediaPacket pkt, long rtpTimestamp) {
        int sz = pkt.getPayload().readableBytes();
        if (sz + 16 > maxRtpSize) {
            writeFuA(buffer, pkt, rtpTimestamp);
        } else {
            writeNalu(buffer, pkt, rtpTimestamp);
        }
    }


    private void writeInterleavedHeader(ByteBuf header, int dataLen) {
        header.writeByte('$');
        header.writeByte(0);
        header.writeShort(dataLen);
    }

    private void writeRtpHeader(ByteBuf header, boolean isKey, long pts) {
        final int version = 2;
        final int payloadType = 98;
        header.writeByte((version & 0x03) << 6);
        header.writeByte(((isKey ? 1 : 0) << 7) | payloadType);
        header.writeShort((int) nextSeqNo());
        header.writeInt((int) (pts));
        header.writeInt(0);
    }

    private void writeNalu(ByteBuf buffer, MediaPacket pkt, long rtpTimestamp) {
        ByteBuf payload = pkt.getPayload().duplicate();

        writeInterleavedHeader(buffer, payload.readableBytes() + 12);
        writeRtpHeader(buffer, pkt.isKey(), rtpTimestamp);
        buffer.writeBytes(payload);
    }

    private void writeFuA(ByteBuf buffer, MediaPacket pkt, long rtpTimestamp) {
        ByteBuf payload = pkt.getPayload().duplicate();

        int sz = payload.readableBytes();
        // FU-A
        // 18 =  4 (interleaved header) +
        //      12 (RTP header)
        //       1 (FU header)
        //       1 (FU indicator)
        int numberOfPackets = (sz - 2) / (maxRtpSize - 18) + 1;

        byte firstByte = payload.readByte();
        int fuIndicator = firstByte & 0b11100000 | 28;
        int fuHeader    = firstByte & 0xff;
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

            int dataLen = Math.min(maxRtpSize - 18, sz - offset);
            writeInterleavedHeader(buffer, dataLen + 12 + 2);
            writeRtpHeader(buffer, pkt.isKey(), rtpTimestamp);

            buffer.writeByte(fuIndicator);
            buffer.writeByte(fuHeader);
            offset += dataLen;

            buffer.writeBytes(payload.readSlice(dataLen));
        }
    }

    public int estimateSize(int payloadSize) {
        if (payloadSize + 16 > maxRtpSize) {
            int numberOfPackets = (payloadSize - 2) / (maxRtpSize - 18) + 1;
            return numberOfPackets * (12 + 2 + 4) + payloadSize;
        } else {
            return payloadSize + 16;
        }
    }

    private long nextSeqNo() {
        return seqNo++;
    }
}
