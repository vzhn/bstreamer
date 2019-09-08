package me.vzhilin.mediaserver.server;

import me.vzhilin.mediaserver.util.PropertyMap;

import java.util.Objects;

public final class SourceKey {
    public final String clazz;
    public final PropertyMap cfg;

    SourceKey(String clazz, PropertyMap cfg) {
        this.clazz = clazz;
        this.cfg = cfg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceKey sourceKey = (SourceKey) o;
        return clazz.equals(sourceKey.clazz) &&
                cfg.equals(sourceKey.cfg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, cfg);
    }
}
