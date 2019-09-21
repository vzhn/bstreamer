package me.vzhilin.bstreamer.server.streaming;

import me.vzhilin.bstreamer.server.ServerContext;
import me.vzhilin.bstreamer.server.stat.GroupStatistics;
import me.vzhilin.bstreamer.server.streaming.picture.AbstractPictureSource;
import me.vzhilin.bstreamer.server.streaming.picture.Typewriter;
import me.vzhilin.bstreamer.util.PropertyMap;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Date;

public class SimplePictureSource extends AbstractPictureSource {
    private final GroupStatistics groupStat;
    private final GroupStatistics totalStat;

    public SimplePictureSource(ServerContext context, PropertyMap properties) {
        super(context, properties);

        totalStat = context.getStat().getTotal();
        groupStat = context.getStat().get(properties);
    }

    @Override
    protected void drawPicture(BufferedImage image) {
        Graphics gc = image.getGraphics();
        gc.setColor(Color.WHITE);
        gc.fillRect(0, 0, image.getWidth(), image.getHeight());

        drawStat(gc);
    }


    private void drawStat(Graphics gc) {
        OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
        gc.setColor(Color.PINK);
        gc.fillRect(0, 0, 220, 90);

        gc.setColor(Color.BLACK);
        Typewriter tw = new Typewriter(gc);

        tw.drawString(new Date().toString());
        tw.drawString("nframe: " + getFrameNumber());
        tw.drawString("connections: " + groupStat.connections());
        tw.drawString("total_connections: " + totalStat.connections());
        tw.drawString("load_average: " + bean.getSystemLoadAverage());
    }
}
