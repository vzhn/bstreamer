package me.vzhilin.mediaserver.stream;

public class Stream<T> {
    private final ItemFactory<T> factory;
    private Node<T> head;
    private int nsubs = 0;

    public Stream(ItemFactory<T> factory) {
        this.factory = factory;
    }

    public Node<T> allocNode() {
        return head = new Node<>(factory.next(), nsubs);
    }

    public void freeNode(Node<T> node) {
        node.setNext(null);
        factory.free(node.getValue());
    }

    public Cursor<T> cursor() {
        ++nsubs;
        if (head == null) {
            head = allocNode();
        } else {
            head.inc();
        }

        return new Cursor<T>(head, this);
    }

    public void closeCursor(Node<T> head) {
        --nsubs;

        while (head != null) {
            Node<T> nx = head.getNext();
            if (head.dec() == 0) {
                freeNode(head);
            }
            head = nx;
        }
    }
}
