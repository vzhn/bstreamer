package me.vzhilin.mediaserver.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import me.vzhilin.mediaserver.media.MediaPacket;

public class RtspServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel channel) {
        final int MTU = 1500;

        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new MessageToByteEncoder<MediaPacket>() {
            int seqNo = 0;
            @Override
            protected void encode(ChannelHandlerContext ctx, MediaPacket next, ByteBuf header) {
                int sz = next.size();
                if (sz + 16 > MTU) {
                    writeFuA(next, header, sz);
                } else {
                    writeNalu(next, header, sz);
                }
            }

            private void writeFuA(MediaPacket next, ByteBuf header, int sz) {
                // FU-A
                // 18 =  4 (interleaved header) +
                //      12 (RTP header)
                //       1 (FU header)
                //       1 (FU indicator)
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
                    } else
                    if (e) {
                        fuHeader |= (1 << 6);
                    }


                    fuHeader |= (r & 1) << 5;

                    int dataLen = Math.min(MTU - 18, sz - offset);

                    writeInterleavedHeader(header, dataLen + 12 + 2);
                    writeRtpHeader(header, next);

                    header.writeByte(fuIndicator);
                    header.writeByte(fuHeader);
                    header.writeBytes(next.getPayload(),  dataLen);

                    offset += dataLen;
                    ++seqNo;
                }
            }

            private void writeNalu(MediaPacket next, ByteBuf header, int sz) {
                // interleaved header
                writeInterleavedHeader(header, sz + 12);

                // RTP header
                writeRtpHeader(header, next);
                header.writeBytes(next.getPayload());
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
        });
        pipeline.addLast(new HttpRequestDecoder());
        pipeline.addLast(new HttpObjectAggregator(16 * 1024));
        pipeline.addLast(new HttpResponseEncoder());
        pipeline.addLast(new RtspServerHandler());
    }
}