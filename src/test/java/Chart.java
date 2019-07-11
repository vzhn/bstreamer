import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

public class Chart {
    /** units per pixel */
    private float factorX = 1f;
    private float factorY = 1f;

    /** anchor point */
    private float bx; // point in chart coordinates
    private float by;
    private float bsx; // same point in screen coordinates
    private float bsy;

    /** axis position in screen coordinates */
    private float axisX;
    private float axisY;

    public void setScaleFactorX(float factorX) {
        this.factorX = factorX;
    }

    public void setScaleFactorY(float factorY) {
        this.factorY = factorY;
    }

    public void bindPoint(float dataX, float dataY, float screenX, float screenY) {
        this.bx = dataX;
        this.by = dataY;
        this.bsx = screenX;
        this.bsy = screenY;
    }

    /**
     * Axis position in pixels
     * @param x
     * @param y
     */
    public void setAxisPosition(float x, float y) {
        this.axisX = x;
        this.axisY = y;
    }

    public void draw(List<DataPoint> points, BufferedImage image) {
        Graphics2D gc = (Graphics2D) image.getGraphics();
        gc.setBackground(Color.WHITE);
        gc.clearRect(0, 0, image.getWidth(), image.getHeight());
        gc.setColor(Color.BLACK);
        points.forEach(dataPoint -> {
            gc.setTransform(new AffineTransform());
            gc.translate(bx / factorX - bsx + axisX, by / factorY - bsy + axisY);
            gc.scale(-factorX, -factorY);
            int x = (int) dataPoint.x;
            int y = (int) dataPoint.y;
            gc.drawLine(x, 0, x, y);
        });
        gc.dispose();
    }

    public void moveAxis(int mx, int my) {
        axisX += mx;
        axisY += my;
    }

    public final static class DataPoint {
        private final float x;
        private final float y;

        DataPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }
    }
}
