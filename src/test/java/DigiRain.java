import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class DigiRain {
    public static final int CHAR_H = 25;
    private final Random rnd;
    private final List<DigiString> strings = new LinkedList<DigiString>();
    Set<Integer> free = new HashSet<>();

    public DigiRain() {
        rnd = new Random();
    }

    public static void main(String... argv) {
        new DigiRain().start();
    }

    private void start() {
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_3BYTE_BGR);
        JFrame frame = new RainFrame(img);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setVisible(true);


        for (int i = 0; i < 800 / 25; i++) {
            free.add(i);
        }


        int w = 25;
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                synchronized (strings) {
                    if (free.isEmpty()) {
                        return;
                    }

                    int y = 0;
                    int k = randomItem(free);
                    free.remove(k);
                    int x = k * 25;
                    strings.add(new DigiString("", x, y));
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
        }, 0, 100, TimeUnit.MILLISECONDS);
        exec.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        synchronized (strings) {
                            strings.forEach(DigiString::appendRandomCharacter);
                        }

                        frame.repaint();
                    }
                }
        , 0, 50, TimeUnit.MILLISECONDS);
    }

    private static class DigiString {
        private final Random rnd;
        private String data;
        private int x;
        private int y;

        public DigiString(String data, int x, int y) {
            this.data = data;
            this.x = x;
            this.y = y;
            this.rnd = new Random();
        }

        public void randomMutate() {
            if (data.length() > 2) {
                int p = rnd.nextInt(data.length());
                data = data.substring(0, p) + randomCharacter() + data.substring(p+1);
            }
        }

        public void appendRandomCharacter() {
//            char ch = (char) ('a' + rnd.nextInt('z' - 'a'));
            char ch = randomCharacter();
            if (data.length() > 20) {
                data = data.substring(1) + ch;
                y += CHAR_H;
            } else {
                data += ch;
            }

        }

        private char randomCharacter() {
            if (rnd.nextFloat() > 0.3) {
                return (char) ('a' + rnd.nextInt('z' - 'a'));
            } else {
                char a = 0xff66;
                char b = 0xff9d;
                return (char)(a + rnd.nextInt(b - a));
            }
        }
    }

    private class RainFrame extends JFrame {
        private final BufferedImage img;
        private final Random rnd;
        private int ticks;

        public RainFrame(BufferedImage img) throws HeadlessException {
            this.img = img;
            this.rnd = new Random();


            this.ticks = 0;
        }

        @Override
        public void paint(Graphics g) {
            ++ticks;
            Graphics gr = img.getGraphics();
            gr.clearRect(0, 0, 800, 600);
            gr.setFont(Font.getFont(Font.MONOSPACED));
            gr.setFont(gr.getFont().deriveFont(25f));
            gr.setColor(Color.GREEN);
            synchronized (strings) {
                strings.forEach(digiString -> paintString(gr, digiString));
                strings.forEach(new Consumer<DigiString>() {
                    @Override
                    public void accept(DigiString digiString) {
                        digiString.randomMutate();
                    }
                });
                strings.forEach(new Consumer<DigiString>() {
                    @Override
                    public void accept(DigiString digiString) {
                        if (digiString.y > 700) {
                            int k = digiString.x / 25;
                            free.add(k);
                        }
                    }
                });
                strings.removeIf(new Predicate<DigiString>() {
                    @Override
                    public boolean test(DigiString digiString) {
                        return digiString.y > 700;
                    }
                });
            }

            gr.dispose();
            blur(g);
        }

        private void blur(Graphics g) {
            int r = 5;
            float[] data = new float[] {
                1, 4, 7, 4, 1,
                4, 16, 26, 16, 4,
                7, 26, 41, 26, 7,
                4, 16, 26, 16, 4,
                1, 4, 7, 4, 1
            };

            for (int i = 0; i < data.length; i++) {
                data[i] *= 1f/273;
            }

//            Kernel kernel = new Kernel(r, r, data);
//            BufferedImageOp op = new ConvolveOp(kernel);

//            BufferedImage rs = op.filter(img, null);
            g.drawImage(img, 0, 0, null);
        }

        private void paintString(Graphics gr, DigiString s) {
            Font font = gr.getFont();
            char[] chs = s.data.toCharArray();

            float factor = 1.0f;
            float[] comp = new float[3];
            for (int i = chs.length - 1; i >= 0; --i) {
                if (factor > 0.0f) {
                    factor -= 0.05f;
                }

                Color green = Color.GREEN;
                green.getRGBColorComponents(comp);
                for (int j = 0; j < 3; j++) {
                    comp[j] *= factor;
                    comp[j] = Math.max(0f, comp[j]);
                }
                if (i == chs.length - 1) {
                    gr.setColor(Color.WHITE);
                } else {
                    gr.setColor(new Color(comp[0], comp[1], comp[2]));
                }
                gr.drawString(String.valueOf(chs[i]), s.x, s.y + CHAR_H * i);
            }
        }
    }
}
