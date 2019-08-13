package me.vzhilin.mediaserver.client.rtsp.messages.sdp;

import java.util.Map;

/**
 * {@code a=fmtp:<format> <format specific parameters>}
 *
 * <p>This attribute allows parameters that are specific to a
 * particular format to be conveyed in a way that SDP doesn't have
 * to understand them.  The format must be one of the formats
 * specified for the media.  Format-specific parameters may be any
 * set of parameters required to be conveyed by SDP and given
 * unchanged to the media tool that will use this format.
 * </p>
 * <p>Created by vzhilin on 08.02.2017.</p>
 */
public class FmtpParameters {
    private final Map<String, String> parameters;

    public FmtpParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    private boolean hasPrameter(String key) {
        String param = parameters.get(key);
        return param != null && !param.isEmpty();
    }

    public Integer packetizationMode() {
        if (hasPrameter("packetization-mode")) {
            return Integer.parseInt(parameters.get("packetization-mode"));
        }

        return -1;
    }

    public Integer profileLevelId() {
        if (hasPrameter("profile-level-id")) {
            return Integer.decode("0x" + parameters.get("profile-level-id"));
        }

        return -1;
    }

    public SpropParameterSets sprops() {
        if (hasPrameter("sprop-parameter-sets")) {
            return new SpropParameterSets(parameters.get("sprop-parameter-sets"));
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FmtpParameters that = (FmtpParameters) o;

        return parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
        return parameters.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e: parameters.entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append(';');
        }

        return sb.substring(0, sb.length()-1);
    }
}
