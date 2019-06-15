package me.vzhilin.mediaserver.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class RtspUriParser {
    private final List<String> pathItems = new ArrayList<>();
    private final String host;
    private final int port;
    private final String uri;

    public RtspUriParser(String uri) throws ParseException {
        this.uri = uri;

        LineParser ps = new LineParser(uri);
        ps.readIgnoreCase("rtsp://");

        host = ps.readTo(':', '/');
        if (ps.ch() == ':') {
            ps.next();
            port = ps.readInt();
        } else {
            port = 554;
        }

        while (ps.hasNext() && ps.ch() == '/') {
            ps.next();
            pathItems.add(ps.readTo('/'));
        }
    }

    public List<String> getPathItems() {
        return pathItems;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUri() {
        return uri;
    }

    private final static class LineParser {
        private final char[] chars;
        private int pos;

        private LineParser(String line) {
            chars = line.toCharArray();
            pos = 0;
        }

        private char ch() {
            return chars[pos];
        }

        public void next() {
            ++pos;
        }

        private void readIgnoreCase(String word) throws ParseException {
            char[] wordChars = word.toCharArray();
            for (char wordChar : wordChars) {
                readCharIgnoreCase(wordChar);
            }
        }

        private void readCharIgnoreCase(char c) throws ParseException {
            if (Character.toLowerCase(ch()) == Character.toLowerCase(c)) {
                next();
            } else {
                throw new ParseException("expected: '" + c + "'", pos);
            }
        }

        private String readTo(char... chars) {
            StringBuilder sb = new StringBuilder();
            while (hasNext() && !contains(chars, ch())) {
                sb.append(ch());
                next();
            }

            return sb.toString();
        }

        private boolean contains(char[] chars, char ch) {
            for (int i = 0; i < chars.length; i++) {
                if (chars[i] == ch) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasNext() {
            return pos < chars.length;
        }

        private int readInt() {
            StringBuilder sb = new StringBuilder();
            while (hasNext() && ch() >= '0' && ch() <= '9') {
                sb.append(ch());
                next();
            }

            return Integer.parseInt(sb.toString());
        }
    }
}
