package me.vzhilin.mediaserver.client.rtsp;

import java.net.URI;
import java.util.Map;

import me.vzhilin.mediaserver.client.rtsp.messages.DescribeReply;
import me.vzhilin.mediaserver.client.rtsp.messages.GetParameterReply;
import me.vzhilin.mediaserver.client.rtsp.messages.PlayReply;
import me.vzhilin.mediaserver.client.rtsp.messages.SetupReply;

public interface RtspConnection {
    void setup(URI uri, RtspCallback<SetupReply> cb);
    void describe(URI uri, RtspCallback<DescribeReply> cb);
    void play(URI uri, String session, RtspCallback<PlayReply> cb);
    void getParameter(URI uri, String session, RtspCallback<GetParameterReply> cb);
    void setParameter(URI uri, String session, Map<String, String> parameters);
    void disconnect();
}
