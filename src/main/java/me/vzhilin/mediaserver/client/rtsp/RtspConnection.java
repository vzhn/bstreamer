package me.vzhilin.mediaserver.client.rtsp;

import java.util.Map;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import me.vzhilin.mediaserver.client.rtsp.messages.DescribeReply;
import me.vzhilin.mediaserver.client.rtsp.messages.GetParameterReply;
import me.vzhilin.mediaserver.client.rtsp.messages.PlayReply;
import me.vzhilin.mediaserver.client.rtsp.messages.SetupReply;

public interface RtspConnection {
    void setup(String controlUrl, RtspCallback<SetupReply> cb, Consumer<ByteBuf> rtpConsumer, boolean isInterleaved);
    void describe(RtspCallback<DescribeReply> cb);
    void play(String session, RtspCallback<PlayReply> cb);
    void getParameter(String session, RtspCallback<GetParameterReply> cb);
    void setParameter(String session, Map<String, String> parameters);
    void disconnect();
}
