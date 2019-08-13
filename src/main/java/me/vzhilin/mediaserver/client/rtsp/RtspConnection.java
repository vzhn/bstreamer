package me.vzhilin.mediaserver.client.rtsp;

import java.util.Map;

import me.vzhilin.mediaserver.client.rtsp.messages.DescribeReply;
import me.vzhilin.mediaserver.client.rtsp.messages.GetParameterReply;
import me.vzhilin.mediaserver.client.rtsp.messages.PlayReply;
import me.vzhilin.mediaserver.client.rtsp.messages.SetupReply;

public interface RtspConnection {
    void setup(String controlUrl, RtspCallback<SetupReply> cb);
    void describe(RtspCallback<DescribeReply> cb);
    void play(String session, RtspCallback<PlayReply> cb);
    void getParameter(String session, RtspCallback<GetParameterReply> cb);
    void setParameter(String session, Map<String, String> parameters);
    void disconnect();
}
