package me.vzhilin.mediaserver.client.rtsp.messages;

import me.vzhilin.mediaserver.client.rtsp.messages.sdp.SdpMessage;

public final class DescribeReply {
    private final SdpMessage sdpMessage;
    private final String contentBase;

    public DescribeReply(String contentBase, SdpMessage sdpMessage) {
        this.contentBase = contentBase;
        this.sdpMessage = sdpMessage;
    }

    public SdpMessage getSdpMessage() {
        return sdpMessage;
    }

    public String getContentBase() {
        return contentBase;
    }

    @Override
    public String toString() {
        return "DescribeReply{" +
                "sdpMessage=" + sdpMessage +
                '}';
    }
}
