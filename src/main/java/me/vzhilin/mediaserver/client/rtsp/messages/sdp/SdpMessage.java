package me.vzhilin.mediaserver.client.rtsp.messages.sdp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SDP: Session Description Protocol
 * Created by vzhilin on 08.02.2017.
 */
public final class SdpMessage {
    private static final String CRLF = "\r\n";
    private final String sessionName;
    private final List<Media> mediaDescriptions = new ArrayList<>();
    private final Map<String, String> attributes = new HashMap<>();
    private final String control;

    /**
     * Описание сессии
     *
     * @param sessionName имя
     * @param control Control URL Attribute
     */
    public SdpMessage(String sessionName, String control) {
        this.sessionName = sessionName;
        this.control = control;
        addAttribute("control", "*");
    }

    /**
     * Создает копию
     * @param mesg SDP
     */
    public SdpMessage(SdpMessage mesg) {
        this.sessionName = mesg.sessionName;
        this.control = mesg.control;

        for (Media m: mesg.mediaDescriptions) {
            addMedia(m);
        }

        mesg.attributes.forEach(this::addAttribute);
    }

    /**
     * добавляем атрибут
     * @param key ключ
     * @param value значение
     */
    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }

    /**
     * читаем значерие атрибута
     * @param key ключ
     * @return значение
     */
    public String getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Session name
     * @return session name
     */
    public String getSessionName() {
        return sessionName;
    }

    /**
     * Media descriptions
     * @return Media descriptions
     */
    public List<Media> getMediaDescriptions() {
        return mediaDescriptions;
    }

    /**
     * <p>Control URL
     *
     * <p>The "a=control:" attribute is used to convey the control URL. This
     * attribute is used both for the session and media descriptions. If
     * used for individual media, it indicates the URL to be used for
     * controlling that particular media stream. If found at the session
     * level, the attribute indicates the URL for aggregate control.
     *
     * <p>This attribute may contain either relative and absolute URLs,
     * following the rules and conventions set out in RFC 1808 [25].
     * Implementations should look for a base URL in the following order:
     * <ul>
     *  <li>The RTSP Content-Base field</li>
     *  <li>The RTSP Content-Location field</li>
     *  <li>The RTSP request URL</li>
     *  </ul>
     *
     * @return Control URL
     */
    public String getControl() {
        return control;
    }

    public void addMedia(Media media) {
        mediaDescriptions.add(media);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SdpMessage that = (SdpMessage) o;
        return Objects.equals(sessionName, that.sessionName) &&
                Objects.equals(mediaDescriptions, that.mediaDescriptions) &&
                Objects.equals(attributes, that.attributes) &&
                Objects.equals(control, that.control);
    }

    public Media getMedia(String control) {
        for (Media m: mediaDescriptions) {
            if (control.equals(m.getControl())) {
                return m;
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionName, mediaDescriptions, attributes, control);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("v=0\r\n");
        sb.append("o=RTSP 50539017935697 1 IN IP4 0.0.0.0").append(CRLF);
        sb.append("s=").append(sessionName).append(CRLF);

        attributes.forEach((k, v) -> sb.append("a=").append(k).append(':').append(v).append(CRLF));

        for (Media media: mediaDescriptions) {
            sb.append("m=").append(media.getMediaType()).append(" ")
              .append(media.getPort()).append(" ")
              .append(media.getTransport()).append(" ")
              .append(media.getFmtList()).append(CRLF);

            Fmtp fmtp = media.getFmtp();
            RtpMap rtpmap = media.getRtpMap();
            if (fmtp != null) {
                sb.append("a=fmtp:").append(fmtp.getFormat()).append(" ")
                  .append(fmtp.getParameters().toString()).append(CRLF);
            }

            if (rtpmap != null) {
                sb.append("a=rtpmap:").append(rtpmap.getPayloadType()).append(" ")
                    .append(rtpmap.getEncodingName()).append('/').append(rtpmap.getClockRate()).append(CRLF);
            }
            sb.append("a=control:").append(media.getControl()).append(CRLF);
        }

        return sb.toString();
    }
}
