package me.vzhilin.mediaserver.media.picture;

public enum PictureMediaPacketSourceAttributes {
    PICTURE_WIDTH("width", "width"),
    PICTURE_HEIGHT("height", "height"),
    PICTURE_ENCODER_BITRATE("encoder.bitrate",""),
    PICTURE_ENCODER_FPS("encoder.fps", ""),
    PICTURE_ENCODER_GOP_SIZE("encoder.gop_size", ""),
    PICTURE_ENCODER_MAX_B_FRAMES("encoder.max_b_frames", "");

    private final String key;
    private final String desc;

    PictureMediaPacketSourceAttributes(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }
}
