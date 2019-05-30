package me.vzhilin.mediaserver.stream;

public interface ItemFactory<T> {
    T next();
    void free(T item);
}
