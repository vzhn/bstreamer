package me.vzhilin.mediaserver.media;

public class RtpPacket {
    private final Packet pkt;
    private final long rtpTimestamp;
    private final long rtpSeqNo;

    public RtpPacket(Packet pkt, long rtpTimestamp, long rtpSeqNo) {
        this.pkt = pkt;
        this.rtpTimestamp = rtpTimestamp;
        this.rtpSeqNo = rtpSeqNo;
    }

    public Packet getPkt() {
        return pkt;
    }

    public long getRtpTimestamp() {
        return rtpTimestamp;
    }

    public long getRtpSeqNo() {
        return rtpSeqNo;
    }
}
