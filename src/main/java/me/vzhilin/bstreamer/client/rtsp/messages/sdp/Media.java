package me.vzhilin.bstreamer.client.rtsp.messages.sdp;

import java.util.Objects;

/**
 * <p>{@code  m=<media> <port> <transport> <fmt list>}</p>
 * Created by vzhilin on 08.02.2017.
 */
public final class Media {
    private final String mediaType;
    private final String port;
    private final String transport;
    private final String fmtList;
    private final MediaEx mediaEx;

    public Media(String mediaType, String port, String transport, String fmtList) {
        this.mediaType = mediaType;
        this.port = port;
        this.transport = transport;
        this.fmtList = fmtList;

        this.mediaEx = new MediaEx();
    }

    public Media(Media proto) {
        this.mediaType = proto.mediaType;
        this.port = proto.port;
        this.transport = proto.transport;
        this.fmtList = proto.fmtList;
        this.mediaEx = new MediaEx(proto.mediaEx);
    }

    public String getMediaType() {
        return mediaType;
    }
    public String getPort() {
        return port;
    }

    public String getTransport() {
        return transport;
    }

    public String getFmtList() {
        return fmtList;
    }

    public Fmtp getFmtp() {
        return mediaEx.getFmtp();
    }

    public String getControl() {
        return mediaEx.getControl();
    }

    public RtpMap getRtpMap() {
        return mediaEx.getRtpMap();
    }

    public void setRtpMap(RtpMap rtpMap) {
        mediaEx.setRtpMap(rtpMap);
    }

    public void setFmtp(Fmtp fmtp) {
        mediaEx.setFmtp(fmtp);
    }

    public void setControl(String control) {
        mediaEx.setControl(control);
    }

    public boolean isMjpeg() {
        return "video".equals(mediaType) && "JPEG".equals(getRtpMap().getEncodingName());
    }

    public boolean isH264() {
        return "video".equals(mediaType) && "H264".equals(getRtpMap().getEncodingName());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Media media = (Media) o;
        return Objects.equals(mediaType, media.mediaType) &&
                Objects.equals(port, media.port) &&
                Objects.equals(transport, media.transport) &&
                Objects.equals(fmtList, media.fmtList) &&
                Objects.equals(mediaEx, media.mediaEx);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediaType, port, transport, fmtList, mediaEx);
    }
}
