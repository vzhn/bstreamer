package me.vzhilin.mediaserver.media;

public class RtpPacket {
    private final MediaPacket pkt;
    private final long rtpTimestamp;
    private final long rtpSeqNo;

    public RtpPacket(MediaPacket pkt, long rtpTimestamp, long rtpSeqNo) {
        this.pkt = pkt;
        this.rtpTimestamp = rtpTimestamp;
        this.rtpSeqNo = rtpSeqNo;
    }

    public MediaPacket getPkt() {
        return pkt;
    }

    public long getRtpTimestamp() {
        return rtpTimestamp;
    }

    public long getRtpSeqNo() {
        return rtpSeqNo;
    }
}
