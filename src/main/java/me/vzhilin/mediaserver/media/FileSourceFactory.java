package me.vzhilin.mediaserver.media;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class FileSourceFactory implements MediaPacketSourceFactory {
    private final String fileName;

    public FileSourceFactory(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public MediaPacketSource newSource() {
        try {
            return new FileMediaPacketSource(new File(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileSourceFactory that = (FileSourceFactory) o;
        return fileName.equals(that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName);
    }
}
