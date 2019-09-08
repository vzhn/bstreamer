package me.vzhilin.mediaserver.server;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RtspUriParser {
    private final List<String> pathItems = new ArrayList<>();
    private final String host;
    private final int port;
    private final String uri;
    private final Map<String, String> params = new HashMap<>();

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
            pathItems.add(ps.readTo('/', '?'));
        }

        if (ps.hasNext() && ps.ch() == '?') {
            ps.read('?');
            while (ps.hasNext()) {
                String key = ps.readTo('=');
                ps.read('=');
                String value = ps.readTo('&');
                if (ps.hasNext()) {
                    ps.read('&');
                }

                params.put(key, value);
            }
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

    public String pathItem(int i) {
        if (pathItems.size() <= i) {
            return null;
        }
        return pathItems.get(i);
    }

    public int pathItems() {
        return pathItems.size();
    }

    public String getParam(String key) {
        return params.get(key);
    }

    public String getParam(String key, String defaultValue) {
        return params.getOrDefault(key, defaultValue);
    }

    public Map<String, String> allParameters() {
        return params;
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

        public void read(char c) throws ParseException {
            if (ch() != c) {
                throw new ParseException("expected: '" + c + "'", pos);
            }

            next();
        }
    }
}
