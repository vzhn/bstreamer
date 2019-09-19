package me.vzhilin.bstreamer.server.streaming;

import me.vzhilin.bstreamer.server.ServerContext;
import me.vzhilin.bstreamer.server.streaming.picture.AbstractPictureSource;
import me.vzhilin.bstreamer.util.PropertyMap;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SimplePictureSource extends AbstractPictureSource {
    public SimplePictureSource(ServerContext context, PropertyMap properties) {
        super(context, properties);
    }

    @Override
    protected void drawPicture(BufferedImage image) {
        Graphics gc = image.getGraphics();
        gc.setColor(Color.GREEN);
        gc.fillRect(0, 0, image.getWidth(), image.getHeight());
    }
}
