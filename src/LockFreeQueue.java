import java.util.concurrent.atomic.AtomicReference;

/**
 * HSLS Chapter 10, page 237 - 238
 */
public class LockFreeQueue<T> {
    private class Node {
        public T value;
        public AtomicReference<Node> next;

        public Node(T value) {
            this.value = value;
            next = new AtomicReference<Node>(null);
        }
    }

    AtomicReference<Node> head, tail;

    public LockFreeQueue() {
        Node node = new Node(null);
        head = new AtomicReference<>(node);
        tail = new AtomicReference<>(node);
    }

    public void enq(T value) {
        Node node = new Node(value);
        while (true) {
            Node last = tail.get();
            Node next = last.next.get();
            if (last == tail.get()) {
                if (next == null) {
                    if (last.next.compareAndSet(next, node)) {
                        tail.compareAndSet(last, node);
                        return;
                    }
                } else {
                    tail.compareAndSet(last, next);
                }
            }
        }
    }

    public T deq() throws Exception {
        while (true) {
            Node first = head.get();
            Node last = tail.get();
            Node next = first.next.get();

            if (first == head.get()) {
                if (first == last) {
                    if (next == null) {
                        throw new Exception("dequeuing from empty queue!");
                    }
                    tail.compareAndSet(last, next);
                } else {
                    T value = next.value;
                    if (head.compareAndSet(first, next)) {
                        return value;
                    }
                }
            }
        }
    }
}
