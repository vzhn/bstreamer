package me.vzhilin.bstreamer.client.rtsp.messages.sdp;

import java.util.Objects;

/**
 *  <p> This attribute allows parameters that are specific to a
 * particular format to be conveyed in a way that SDP doesn't have
 * to understand them.  The format must be one of the formats
 * specified for the media.  Format-specific parameters may be any
 * set of parameters required to be conveyed by SDP and given
 * unchanged to the media tool that will use this format. </p>
 *
 * {@code a=fmtp:<format> <format specific parameters>}
 *
 * <p>Created by vzhilin on 08.02.2017.</p>
 */
public final class Fmtp {
    private final int format;
    private final FmtpParameters parameters;
    public Fmtp(int format, FmtpParameters parameters) {
        this.format = format;
        this.parameters = parameters;
    }

    public int getFormat() {
        return format;
    }

    public FmtpParameters getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Fmtp fmtp = (Fmtp) o;
        return format == fmtp.format &&
                Objects.equals(parameters, fmtp.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format, parameters);
    }
}
