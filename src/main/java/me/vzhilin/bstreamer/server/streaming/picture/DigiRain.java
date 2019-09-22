package me.vzhilin.bstreamer.server.streaming.picture;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

public class DigiRain {
    private final Random rnd;
    private final List<DigiString> strings = new LinkedList<DigiString>();
    private final Set<Integer> freeColumns = new HashSet<>();
    private final int charWidth;
    private final int charHeight;

    private final Font font;
    private final int width;
    private final int height;
    private final int stringHeight;

    private int tick = 0;
    private int curColumn = 0;

    public DigiRain(int width, int height, int fontSize) {
        this.width = width;
        this.height = height;
        this.font = new Font(Font.MONOSPACED, Font.PLAIN, fontSize);
        rnd = new Random();

        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_3BYTE_BGR);
        Graphics gc = img.getGraphics();
        gc.setFont(this.font);

        FontMetrics fm = gc.getFontMetrics();
        charHeight = fm.getAscent() + fm.getDescent();
        charWidth = charHeight;
        gc.dispose();

        for (int i = 0; i < width / charWidth; i++) {
            freeColumns.add(i);
        }

        this.stringHeight = height / charHeight;
    }

    public synchronized void tick() {
        ++tick;

        int cols = width / charWidth;
        int rows = height / charHeight + stringHeight;

        int nRow = tick % rows;
        int nextCol = Math.round((float) cols * nRow / rows);
        if (nextCol != curColumn) {
            int inc = (cols + nextCol - curColumn) % cols;
            curColumn = nextCol;
            for (int i = 0; i < inc; i++) {
                if (freeColumns.isEmpty()) {
                    break;
                }
                int freeColumn = randomItem(freeColumns);
                freeColumns.remove(freeColumn);
                strings.add(new DigiString(freeColumn));
            }
        }

        for (Iterator<DigiString> iterator = strings.iterator(); iterator.hasNext(); ) {
            DigiString digiString = iterator.next();
            if (digiString.y + stringHeight > height) {
                freeColumns.add(digiString.column);
                iterator.remove();
            } else {
                digiString.randomMutate();
                digiString.appendRandomCharacter();
            }
        }
    }

    private int randomItem(Set<Integer> free) {
        Iterator<Integer> it = free.iterator();
        int pos = rnd.nextInt(free.size());
        int v = it.next();
        for (int i = 0; i < pos - 1; i++) {
            v = it.next();
        }
        return v;
    }

    public synchronized void paint(Graphics gc) {
        gc.setFont(font);
        strings.forEach(digiString -> digiString.paint(gc));
    }

    private class DigiString {
        private final Random rnd;
        private final int column;
        private String data;
        private int x;
        private int y;

        private DigiString(int column) {
            this.data = "";
            this.column = column;
            this.x = column * charWidth;
            this.y = 0;
            this.rnd = new Random();
        }

        private void randomMutate() {
            if (data.length() > 2) {
                int p = rnd.nextInt(data.length());
                data = data.substring(0, p) + randomCharacter() + data.substring(p+1);
            }
        }

        private void appendRandomCharacter() {
            char ch = randomCharacter();
            if (data.length() > stringHeight) {
                data = data.substring(1) + ch;
                y += charHeight;
            } else {
                data += ch;
            }

        }

        private char randomCharacter() {
            if (rnd.nextFloat() > 0.3) {
                return (char) ('a' + rnd.nextInt('z' - 'a'));
            } else {
                char from = 0xff66;
                char to = 0xff9d;
                return (char)(from + rnd.nextInt(to - from));
            }
        }

        private void paint(Graphics gr) {
            char[] chs = data.toCharArray();
            float factor = 1.0f;
            float[] comp = new float[3];

            float step = 1.0f / stringHeight;
            for (int i = chs.length - 1; i >= 0; --i) {
                if (i == chs.length - 1) {
                    gr.setColor(Color.WHITE);
                } else {
                    if (factor > step) {
                        factor -= step;
                        Color green = Color.GREEN;
                        green.getRGBColorComponents(comp);
                        for (int j = 0; j < 3; j++) {
                            comp[j] *= factor;
                            comp[j] = Math.max(0f, comp[j]);
                        }
                        gr.setColor(new Color(comp[0], comp[1], comp[2]));
                    } else {
                        break;
                    }
                }

                gr.drawString(String.valueOf(chs[i]), x, y + charHeight * i);
            }
        }
    }
}
