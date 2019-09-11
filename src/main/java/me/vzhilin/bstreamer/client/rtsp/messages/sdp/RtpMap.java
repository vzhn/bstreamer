package me.vzhilin.bstreamer.client.rtsp.messages.sdp;

import java.util.Objects;

/**
 * For RTP formats using a dynamic
 * payload type number, the dynamic payload type number is given as
 * the format and an additional "rtpmap" attribute specifies the
 * format and parameters.
 * a=rtpmap:(payload type) (encoding name)/(clock rate) [/&lt;encoding parameters&gt;]
 * Created by vzhilin on 08.02.2017.
 */
public final class RtpMap {
    private final int payloadType;
    private final String encodingName;
    private final int clockRate;

    public RtpMap(int payloadType, String encodingName, int clockRate) {
        this.payloadType = payloadType;
        this.encodingName = encodingName;
        this.clockRate = clockRate;
    }

    public int getPayloadType() {
        return payloadType;
    }

    public String getEncodingName() {
        return encodingName;
    }

    public int getClockRate() {
        return clockRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RtpMap rtpMap = (RtpMap) o;
        return payloadType == rtpMap.payloadType &&
                clockRate == rtpMap.clockRate &&
                Objects.equals(encodingName, rtpMap.encodingName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payloadType, encodingName, clockRate);
    }
}
