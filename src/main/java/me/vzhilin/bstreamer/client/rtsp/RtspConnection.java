package me.vzhilin.bstreamer.client.rtsp;

import me.vzhilin.bstreamer.client.rtsp.messages.DescribeReply;
import me.vzhilin.bstreamer.client.rtsp.messages.GetParameterReply;
import me.vzhilin.bstreamer.client.rtsp.messages.PlayReply;
import me.vzhilin.bstreamer.client.rtsp.messages.SetupReply;

import java.net.URI;
import java.util.Map;

public interface RtspConnection {
    void setup(URI uri, RtspCallback<SetupReply> cb);
    void describe(URI uri, RtspCallback<DescribeReply> cb);
    void play(URI uri, String session, RtspCallback<PlayReply> cb);
    void getParameter(URI uri, String session, RtspCallback<GetParameterReply> cb);
    void setParameter(URI uri, String session, Map<String, String> parameters);
    void disconnect();
}
