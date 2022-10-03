public class LinkedListDeque<T> implements Deque<T> {
    private class TN {
        private T item;
        private TN prev;
        private TN next;

        private TN(T a, TN b, TN c) {
            item = a;
            prev = b;
            next = c;
        }

    }

    private int size;
    private TN santinel;

    public LinkedListDeque() {
        size = 0;
        santinel = new TN(null, null, null);
        santinel.prev = santinel;
        santinel.next = santinel;
    }
    @Override
    public int size() {
        return size;
    }
    @Override
    public boolean isEmpty() {
        return size == 0;
    }
    @Override
    public void addFirst(T item) {
        santinel.next = new TN(item, santinel, santinel.next);
        santinel.next.next.prev = santinel.next;
        size += 1;
    }
    @Override
    public void addLast(T item) {
        santinel.prev = new TN(item, santinel.prev, santinel);
        santinel.prev.prev.next = santinel.prev;
        size += 1;
    }
    @Override
    public T removeFirst() {
        T rest = santinel.next.item;
        santinel.next.next.prev = santinel;
        santinel.next = santinel.next.next;
        if (!isEmpty()) {
            size -= 1;
        }
        return rest;
    }
    @Override
    public T removeLast() {
        T rest = santinel.prev.item;
        santinel.prev.prev.next = santinel;
        santinel.prev = santinel.prev.prev;
        if (!isEmpty()) {
            size -= 1;
        }
        return rest;
    }

    public void printDeque() {
        TN print = santinel.next;
        for (int i = 0; i < size; i++) {
            System.out.print(print.item + " ");
            print = print.next;
        }
        System.out.println();
    }
    @Override
    public T get(int index) {
        TN get = santinel.next;
        for (int i = 0; i < index; i++) {
            get = get.next;
        }
        return get.item;
    }

    private T getRecursive(int index, TN tmp) {
        if (index == 0) {
            return tmp.item;
        }
        return getRecursive(index - 1, tmp.next);
    }

    public T getRecursive(int index) {
        return getRecursive(index, santinel.next);
    }

}

