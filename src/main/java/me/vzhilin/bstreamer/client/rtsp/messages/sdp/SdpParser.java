package me.vzhilin.bstreamer.client.rtsp.messages.sdp;

import java.util.*;

public final class SdpParser {
    public SdpMessage parse(String text) {
        Scanner sc = new Scanner(text);
        List<Media> ms = new ArrayList<>();
        String sessionName = "";
        String sessionControl = "*";

        String line;
        while (sc.hasNextLine()) {
            line = sc.nextLine();

            if (line.startsWith("s=")) {
                sessionName = line.substring(2);
                continue;
            }

            if (line.startsWith("a=control:")) {
                sessionControl = line.substring("a=control:".length());
                continue;
            }

            if (line.startsWith("m=")) {
                parseMedia(ms, sc, line);
            }
        }

        SdpMessage message = new SdpMessage(sessionName, sessionControl);
        for (Media m: ms) {
            message.addMedia(m);
        }

        return message;
    }

    private void parseMedia(List<Media> ms, Scanner sc, final String text) {
        String mediaLine = text.substring(2);
        String[] parts = mediaLine.split(" ");
        Media media = new Media(parts[0], parts[1], parts[2], parts[3]);
        ms.add(media);

        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.startsWith("a=rtpmap:")) {
                String rtpLine = line.substring("a=rtpmap:".length());
                String[] ps = rtpLine.split(" ");

                int payloadType = Integer.parseInt(ps[0]);
                String encodingName = ps[1].split("/")[0];
                int clockRate = Integer.parseInt(ps[1].split("/")[1]);

                RtpMap rtpMap = new RtpMap(payloadType, encodingName, clockRate);
                media.setRtpMap(rtpMap);

                continue;
            }

            if (line.startsWith("a=fmtp:")) {
                String fmtpLine = line.substring("a=fmtp:".length());
                int ix = fmtpLine.indexOf(' ');
                String p1 = fmtpLine.substring(0, ix);
                String p2 = fmtpLine.substring(ix+1);
                int payloadType = Integer.parseInt(p1);
                Map<String, String> params = new LinkedHashMap<>();
                for (String param : p2.split(";")) {
                    String k = param.substring(0, param.indexOf('=')).trim();
                    String v = param.substring(param.indexOf('=')+1).trim();

                    params.put(k, v);
                }

                media.setFmtp(new Fmtp(payloadType, new FmtpParameters(params)));
                continue;
            }
            if (line.startsWith("a=control:")) {
                media.setControl(line.substring("a=control:".length()));
                continue;
            }
            if (line.startsWith("m=")) {
                parseMedia(ms, sc, line);
            }
        }
    }
}
