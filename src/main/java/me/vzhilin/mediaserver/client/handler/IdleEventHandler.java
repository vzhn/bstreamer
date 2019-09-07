package me.vzhilin.mediaserver.client.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import me.vzhilin.mediaserver.client.Client;
import me.vzhilin.mediaserver.client.ConnectionStatistics;

@ChannelHandler.Sharable
final class IdleEventHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ConnectionStatistics connectionStat = ctx.channel().attr(Client.STAT).get();
            connectionStat.onIdleError();
            ctx.close();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
