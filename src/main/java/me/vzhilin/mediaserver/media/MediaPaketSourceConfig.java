package me.vzhilin.mediaserver.media;

import me.vzhilin.mediaserver.conf.PropertyMap;

import java.io.File;

public class MediaPaketSourceConfig {
    public static final String NAME = "source";

    public static final String FILE_DIR                     = "dir";
    public static final String PICTURE_WIDTH                = "picture.width";
    public static final String PICTURE_HEIGHT               = "picture.height";
    public static final String PICTURE_ENCODER_BITRATE      = "picture.encoder.bitrate";
    public static final String PICTURE_ENCODER_FPS          = "picture.encoder.fps";
    public static final String PICTURE_ENCODER_GOP_SIZE     = "picture.encoder.gop_size";
    public static final String PICTURE_ENCODER_MAX_B_FRAMES = "picture.encoder.max_b_frames";

    public static final String EXTRA = "_extra";
    private final PropertyMap properties;

    public MediaPaketSourceConfig(PropertyMap propertyMap) {
        this.properties = propertyMap;
    }

    public String getName() {
        return properties.getValue(NAME);
    }

    public void setName(String name) {
        properties.put(NAME, name);
    }

    public File getFileDir() {
        return new File(properties.getValue(FILE_DIR));
    }

    public void setExtra(String extra) {
        properties.put(EXTRA, extra);
    }

    public String getExtra() {
        return properties.getValue(EXTRA);
    }
}
