package me.vzhilin.bstreamer.server.streaming;

import me.vzhilin.bstreamer.server.ServerContext;
import me.vzhilin.bstreamer.server.stat.GroupStatistics;
import me.vzhilin.bstreamer.server.streaming.picture.AbstractPictureSource;
import me.vzhilin.bstreamer.server.streaming.picture.DigiRain;
import me.vzhilin.bstreamer.server.streaming.picture.PictureSourceAttributes;
import me.vzhilin.bstreamer.server.streaming.picture.Typewriter;
import me.vzhilin.bstreamer.util.PropertyMap;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Date;

public class Filesystem extends AbstractPictureSource {
    private final GroupStatistics groupStat;
    private final GroupStatistics totalStat;
    private final int width;
    private final int height;
    private final DigiRain dr;

    public Filesystem(ServerContext context, PropertyMap properties) {
        super(context, properties);

        width = properties.getInt(PictureSourceAttributes.PICTURE_WIDTH);
        height = properties.getInt(PictureSourceAttributes.PICTURE_HEIGHT);

        totalStat = context.getStat().getTotal();
        groupStat = context.getStat().get(properties);
        dr = new DigiRain(width, height, 25);
    }

    @Override
    protected void drawPicture(BufferedImage image) {
        dr.tick();

        Graphics gc = image.getGraphics();
        gc.setColor(Color.BLACK);
        gc.fillRect(0, 0, image.getWidth(), image.getHeight());
        drawStat(gc);
    }

    private void drawStat(Graphics gc) {
        Font font = gc.getFont();
        dr.paint(gc);
        gc.setFont(font);

        OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
        gc.setColor(Color.LIGHT_GRAY);
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
