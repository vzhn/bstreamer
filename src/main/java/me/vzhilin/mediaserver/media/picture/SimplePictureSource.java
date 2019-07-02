package me.vzhilin.mediaserver.media.picture;

import com.codahale.metrics.Meter;
import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.server.ServerContext;
import me.vzhilin.mediaserver.server.stat.GroupStatistics;
import me.vzhilin.mediaserver.server.stat.ServerStatistics;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SimplePictureSource extends AbstractPictureSource {
    private final ServerStatistics stat;

    public SimplePictureSource(ServerContext context, PropertyMap properties) {
        super(context, properties);
        stat = context.getStat();
    }

    @Override
    protected void drawPicture(BufferedImage image)  {


        Meter meter = stat.getThroughputMeter();;

        Graphics gc = image.getGraphics();
        gc.setColor(Color.WHITE);
        gc.fillRect(0, 0, image.getWidth(), image.getHeight());
        gc.setColor(Color.BLACK);
        gc.setFont(gc.getFont().deriveFont(35f));

        GroupStatistics groupStat = getGroupStatistics();
        gc.drawString("GroupClientCount: " + groupStat.getClientCount(), 10, 100);
        gc.drawString("TotalGroupCount: " + stat.getGroupCount(), 10, 150);
        gc.drawString("TotalClientCount: " + stat.getClientCount(), 10, 200);
        gc.drawString(String.format("total throughupt: %.2f", meter.getOneMinuteRate()), 10, 250);
        gc.dispose();
    }
}
