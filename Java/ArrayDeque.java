public class ArrayDeque<T> implements Deque<T> {

    private T[] item;
    private int first;
    private int last;
    private int size;

    public ArrayDeque() {
        item = (T[]) new Object[8];
        first = 0;
        last = 1;
        size = 0;
    }

    private boolean isFull() {
        return size == item.length;
    }

    private boolean isDown() {
        return item.length >= 16 && size < (item.length / 4);
    }

    private int oneMore(int index) {
        return (index + 1) % item.length;
    }

    private int oneLess(int index) {
        return (index - 1 + item.length) % item.length;
    }

    private void resize(int capacity) {
        T[] nDeque = (T[]) new Object[capacity];
        int oIndex = oneMore(first);
        for (int nIndex = 0; nIndex < size; nIndex++) {
            nDeque[nIndex] = item[oIndex];
            oIndex = oneMore(oIndex);
        }
        item = nDeque;
        first = capacity - 1;
        last = size;

    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }
    @Override
    public int size() {
        return size;
    }

    private void bigSize() {
        resize(size * 2);
    }

    private void smallSize() {
        resize(item.length / 2);
    }

    public void printDeque() {
        for (int i = oneMore(first); i != last; i = oneMore(i)) {
            System.out.print(item[i] + " ");
        }
        System.out.println();
    }
    @Override
    public void addFirst(T x) {
        if (isFull()) {
            bigSize();
        }
        item[first] = x;
        first = oneLess(first);
        size += 1;
    }
    @Override
    public void addLast(T x) {
        if (isFull()) {
            bigSize();
        }
        item[last] = x;
        last = oneMore(last);
        size += 1;
    }
    @Override
    public T removeFirst() {
        if (isDown()) {
            smallSize();
        }
        first = oneMore(first);
        T rest = item[first];
        item[first] = null;
        if (!isEmpty()) {
            size -= 1;
        }
        return rest;
    }
    @Override
    public T removeLast() {
        if (isDown()) {
            smallSize();
        }
        last = oneLess(last);
        T rest = item[last];
        item[last] = null;
        if (!isEmpty()) {
            size -= 1;
        }
        return rest;
    }
    @Override
    public T get(int index) {
        if (index >= size) {
            return null;
        }
        int start = oneMore(first);
        return item[(start + index) % item.length];
    }

}
