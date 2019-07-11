import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChartRender {
    public static final int CHART_WIDTH = 800;
    public static final int CHART_HEIGHT = 600;

    public static final int RIGHT_MARGIN = 50;
    public static final int BOTTOM_MARGIN = 40;
    private final Chart chart;
    private BufferedImage img;

    private final List<Chart.DataPoint> points;

    public static void main(String... argv) {
        new ChartRender().start();
    }

    public ChartRender() {
        this.img = new BufferedImage(CHART_WIDTH, CHART_HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
        this.chart = new Chart();
        points = new ArrayList<>();
        fillPoints();
    }

    private void fillPoints() {
        long now = 0;

        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            points.add(new Chart.DataPoint(i, random.nextInt(100)));
        }
    }

    private void start() {

        JFrame frame = new MyJFrame();
        frame.setSize(CHART_WIDTH, CHART_HEIGHT);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);

        SwingUtilities.invokeLater(frame::repaint);
    }

    private class MyJFrame extends JFrame {
        private int k = 0;
        public MyJFrame() throws HeadlessException {
            addMouseWheelListener(e -> {
                k += e.getWheelRotation();
                chart.setScaleFactorX((float) Math.pow(2f, 0.2f * k));
                chart.setScaleFactorY((float) Math.pow(2f, 0.2f * k));
                repaint();
            });

            MouseAdapter adapter = new MouseAdapter() {
                private int py;
                private int px;

                @Override
                public void mouseDragged(MouseEvent e) {
                    int mx = e.getX();
                    int my = e.getY();
                    int dx = mx - px;
                    int dy = my - py;
                    px = mx;
                    py = my;

                    chart.moveAxis(dx, dy);
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    px = e.getX();
                    py = e.getY();
                }
            };
            addMouseListener(adapter);
            addMouseMotionListener(adapter);
        }

        @Override
        public void paint(Graphics g) {
            chart.draw(points, img);
            g.drawImage(img, 0, 0, null);
        }
    }
}

