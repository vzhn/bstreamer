package me.vzhilin.bstreamer.client.rtsp.messages.sdp;

import java.util.Objects;

final class MediaEx {
    private Fmtp fmtp;
    private String control;
    private RtpMap rtpMap;

    MediaEx(MediaEx mediaEx) {
        this.fmtp = mediaEx.fmtp;
        this.control = mediaEx.control;
        this.rtpMap = mediaEx.rtpMap;
    }

    MediaEx() {

    }

    Fmtp getFmtp() {
        return fmtp;
    }

    void setFmtp(Fmtp fmtp) {
        this.fmtp = fmtp;
    }

    public String getControl() {
        return control;
    }

    void setControl(String control) {
        this.control = control;
    }

    RtpMap getRtpMap() {
        return rtpMap;
    }

    void setRtpMap(RtpMap rtpMap) {
        this.rtpMap = rtpMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MediaEx mediaEx = (MediaEx) o;
        return Objects.equals(fmtp, mediaEx.fmtp) &&
                Objects.equals(control, mediaEx.control) &&
                Objects.equals(rtpMap, mediaEx.rtpMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fmtp, control, rtpMap);
    }
}
