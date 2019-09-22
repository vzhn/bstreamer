import me.vzhilin.bstreamer.server.streaming.picture.DigiRain;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class DigiRainDemo {
    private final int width;
    private final int height;

    private DigiRainDemo(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public static void main(String... argv) {
        new DigiRainDemo(2000, 1200).start();
    }

    private void start() {
        DigiRain dr = new DigiRain(width, height, 25);

        JFrame frame = new RainFrame(dr);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.setVisible(true);

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(() -> {
            SwingUtilities.invokeLater(dr::tick);
            frame.repaint();
        }, 0, 1000 / 25, TimeUnit.MILLISECONDS);
    }

    private class RainFrame extends JFrame {
        private final BufferedImage img;
        private final DigiRain digiRain;

        public RainFrame(DigiRain dr) {
            this.digiRain = dr;
            img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        }

        @Override
        public void paint(Graphics g) {
            Graphics gc = img.getGraphics();
            gc.clearRect(0, 0, width, height);
            digiRain.paint(gc);
            gc.dispose();

            g.drawImage(img, 0, 0, null);
        }
    }
}
