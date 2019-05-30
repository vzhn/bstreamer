package me.vzhilin.mediaserver.stream;

public class Node<T> {
    private T value;
    private int nsubs;
    private Node<T> next;

    public Node(T value, int nsubs) {
        this.value = value;
        this.nsubs = nsubs;
    }

    public T getValue() {
        return value;
    }

    public boolean hasNext() {
        return next != null;
    }

    public Node<T> getNext() {
        return next;
    }

    public Node<T> setNext(Node<T> next) {
        return this.next = next;
    }

    public int dec() {
        return --nsubs;
    }

    public void inc() {
        ++nsubs;
    }

    public int getNsubs() {
        return nsubs;
    }
}
