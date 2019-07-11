package me.vzhilin.mediaserver.media.picture;

import com.codahale.metrics.Meter;
import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.server.ServerContext;
import me.vzhilin.mediaserver.server.stat.GroupStatistics;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;
import me.vzhilin.mediaserver.server.stat.TimeSeries;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class SimplePictureSource extends AbstractPictureSource {
    private final ServerStatistics stat;
    private final List<TimeSeries.TimeSeriesEntry> ts;
    private final Chart chart;

    private final int padding = 5;

    public SimplePictureSource(ServerContext context, PropertyMap properties) {
        super(context, properties);
        stat = context.getStat();
        ts = new ArrayList<>();
        this.chart = new Chart(stat);
    }

    @Override
    protected void drawPicture(BufferedImage image)  {
        ts.clear();
        stat.drainTs(ts);
        BufferedImage bi = chart.createChart(chart.createDataset(ts)).createBufferedImage(image.getWidth(), image.getHeight());
        Meter meter = stat.getThroughputMeter();;
        Graphics2D gc = (Graphics2D) image.getGraphics();
        gc.drawImage(bi, 0, 0, null);
        gc.setColor(Color.BLACK);
        AffineTransform transform = gc.getTransform();
        gc.setTransform(transform);

        int y = 50;
        int x = image.getWidth() - 250 - 2 * padding;

        drawStat(meter, gc, x, y);
        gc.dispose();
    }

    private void drawStat(Meter meter, Graphics2D gc, int x, int y) {
        GroupStatistics groupStat = getGroupStatistics();
        FontMetrics fm = gc.getFontMetrics();
        int height = fm.getHeight();

        gc.setBackground(Color.YELLOW);
        gc.clearRect(x - padding, y - padding, 200 + 2 * padding, 4 * height + 2 * padding);

        y += height;
        gc.drawString("client_count: " + groupStat.getClientCount(), x, y);
        y += height;

        gc.drawString("group_count: " + stat.getGroupCount(), x, y);
        y += height;

        gc.drawString("total_client_count: " + stat.getClientCount(), x, y);
        y += height;

        gc.drawString(String.format("total throughupt: %s", humanReadableByteCount((long) meter.getOneMinuteRate(), false)), x, y);
    }

    private final static class Chart {
        private final ServerStatistics stat;
        private final Font serif = new Font("Serif", Font.BOLD, 18);
        private final List<TimeSeries.TimeSeriesEntry> ts = new ArrayList<>();

        private Chart(ServerStatistics stat) throws HeadlessException {
            this.stat = stat;
            JFreeChart chart = createChart(createDataset(ts));
        }

        private IntervalXYDataset createDataset(List<TimeSeries.TimeSeriesEntry> ts) {
            XYSeries series = new XYSeries("2016");
            XYSeriesCollection dataset = new XYSeriesCollection();
            dataset.addSeries(series);
            if (!ts.isEmpty()) {
                TimeSeries.TimeSeriesEntry t = ts.get(ts.size() - 1);
                long timeMillis = t.getTimeMillis();
                ts.forEach(new Consumer<TimeSeries.TimeSeriesEntry>() {
                    @Override
                    public void accept(TimeSeries.TimeSeriesEntry timeSeriesEntry) {
                        series.add(timeMillis - timeSeriesEntry.getTimeMillis(), timeSeriesEntry.getPayloadSize());
                    }
                });
            }
            return dataset;
        }

        private JFreeChart createChart(IntervalXYDataset dataset) {
            JFreeChart chart = ChartFactory.createXYLineChart(
                    "Traffic",
                    "time",
                    "Traffic",
                    dataset,
                    PlotOrientation.VERTICAL,
                    false,
                    true,
                    false
            );
            XYPlot plot = chart.getXYPlot();
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setSeriesPaint(0, Color.RED);
            renderer.setSeriesStroke(0, new BasicStroke(2.0f));
            plot.setRenderer(renderer);
            plot.setBackgroundPaint(Color.white);
            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.BLACK);
            plot.setDomainGridlinesVisible(true);
            plot.setDomainGridlinePaint(Color.BLACK);
            String title = new Date().toString();
            chart.setTitle(new TextTitle(title, serif));
            return chart;
        }
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
