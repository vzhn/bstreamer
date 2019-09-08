package me.vzhilin.mediaserver.media.impl.picture;

import me.vzhilin.mediaserver.conf.PropertyMap;
import me.vzhilin.mediaserver.server.ServerContext;

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
