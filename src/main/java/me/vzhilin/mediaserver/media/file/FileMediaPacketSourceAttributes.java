package me.vzhilin.mediaserver.media.file;

public enum FileMediaPacketSourceAttributes {
    FILE_DIR("dir", "video directory");

    private final String key;
    private final String desc;
    FileMediaPacketSourceAttributes(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }
}
