package me.vzhilin.mediaserver.media;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class FileSourceFactory implements MediaPacketSourceFactory {
    private final File file;

    public FileSourceFactory(File file) {
        this.file = file;
    }

    @Override
    public MediaPacketSource newSource() {
        try {
            return new FileMediaPacketSource(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileSourceFactory that = (FileSourceFactory) o;
        return Objects.equals(file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file);
    }
}
