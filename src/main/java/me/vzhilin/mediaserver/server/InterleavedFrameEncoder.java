package me.vzhilin.mediaserver.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import me.vzhilin.mediaserver.server.media.InterleavedFrame;

public class InterleavedFrameEncoder extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof InterleavedFrame) {
            ctx.write(((InterleavedFrame) msg).getPayload().retainedDuplicate(), promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }
}
