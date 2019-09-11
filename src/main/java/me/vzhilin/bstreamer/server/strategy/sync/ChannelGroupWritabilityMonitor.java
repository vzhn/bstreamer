package me.vzhilin.bstreamer.server.strategy.sync;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.HashSet;
import java.util.Set;

@ChannelHandler.Sharable
final class ChannelGroupWritabilityMonitor extends ChannelInboundHandlerAdapter {
    private final Runnable onWritable;
    private final Runnable onUnwritable;
    private Set<ChannelHandlerContext> unwritable = new HashSet<>();
    private volatile boolean writable;
    private int totalChannels;

    ChannelGroupWritabilityMonitor(Runnable onWritable, Runnable onUnwritable) {
        this.onWritable = onWritable;
        this.onUnwritable = onUnwritable;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        ++totalChannels;
        if (!ctx.channel().isWritable()) {
            addToUnwritable(ctx);
        } else {
            if (!writable && unwritable.isEmpty()) {
                writable = true;
                onWritable.run();
            }
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        --totalChannels;
        removeFromUnwritable(ctx);
        if (totalChannels == 0 && writable) {
            writable = false;
            onUnwritable.run();
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        if (ctx.channel().isWritable()) {
            removeFromUnwritable(ctx);
        } else {
            addToUnwritable(ctx);
        }
    }

    private void addToUnwritable(ChannelHandlerContext ctx) {
        unwritable.add(ctx);
        if (writable) {
            writable = false;
            onUnwritable.run();
        }
    }

    private void removeFromUnwritable(ChannelHandlerContext ctx) {
        unwritable.remove(ctx);
        if (!writable && unwritable.isEmpty() && totalChannels > 0) {
            writable = true;
            onWritable.run();
        }
    }

    boolean isWritable() {
        return writable;
    }
}
