package me.vzhilin.bstreamer.server.streaming.picture;

import java.awt.*;

public final class Typewriter {
    private final Graphics gc;
    private final int lineHeight;
    private final int hPosition;
    private int vPosition;

    public Typewriter(Graphics gc) {
        this.gc = gc;
        FontMetrics fm = gc.getFontMetrics();
        this.lineHeight = fm.getAscent() + fm.getDescent();
        this.vPosition = lineHeight;
        this.hPosition = 10;
    }

    public void drawString(String text) {
        gc.drawString(text, hPosition, vPosition);
        vPosition += lineHeight;
    }
}
