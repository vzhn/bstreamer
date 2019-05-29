package me.vzhilin.mediaserver.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import me.vzhilin.mediaserver.media.MediaPacket;

import javax.print.attribute.standard.Media;
import java.nio.charset.Charset;

public class RtspServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new MessageToByteEncoder<MediaPacket>() {
            int seqNo = 0;
            @Override
            protected void encode(ChannelHandlerContext ctx, MediaPacket next, ByteBuf header) throws Exception {
//                header.writeCharSequence("$Asdasdasd", Charset.defaultCharset());
//                ByteBuf header = Unpooled.buffer();
                header.writeByte('$');
                header.writeByte(0);
                header.writeShort(next.size() + 12);

                // RTP header
                final int version = 2;
                final int payloadType = 98;
                header.writeByte((version & 0x03) << 6);
                header.writeByte(((next.isKey()?1:0) << 7) | payloadType);
                header.writeShort(seqNo++);
                header.writeInt((int) (next.getPts() * 90));
                header.writeInt(0);
                header.writeBytes(next.getPayload());
//                ctx.writeAndFlush(header);
            }
        });
        pipeline.addLast(new HttpRequestDecoder());
        pipeline.addLast(new HttpObjectAggregator(16 * 1024));
        pipeline.addLast(new HttpResponseEncoder());
        pipeline.addLast(new RtspServerHandler());
    }
}