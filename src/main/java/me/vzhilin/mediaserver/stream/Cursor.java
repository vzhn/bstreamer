package me.vzhilin.mediaserver.stream;

public class Cursor<T> {
    private final Stream<T> stream;
    private Node<T> head;

    public Cursor(Node<T> head, Stream<T> stream) {
        this.stream = stream;
        this.head = head;
    }

    public void close() {
        stream.closeCursor(head);
    }

    public T next() {
        T v = head.getValue();

        Node cur = head;
        if (!head.hasNext()) {
            Node<T> next = stream.allocNode();
            head = head.setNext(next);
        } else {
            head = head.getNext();
        }

        if (cur.dec() == 0) {
            stream.freeNode(cur);
        }

        return v;
    }

}
