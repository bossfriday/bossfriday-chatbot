package cn.bossfriday.chatbot.common;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ConcurrentCircularList
 *
 * @author chenx
 */
public class ConcurrentCircularList<T> implements Iterable<T> {

    private Object[] elements;
    private int size;
    private int headIndex;
    private int tailIndex;
    private Lock lock = new ReentrantLock();

    public ConcurrentCircularList(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }

        this.elements = new Object[capacity];
        this.size = 0;
        this.headIndex = 0;
        this.tailIndex = 0;
    }

    /**
     * add
     *
     * @param element
     */
    public void add(T element) {
        this.lock.lock();
        try {
            this.elements[this.tailIndex] = element;
            if (this.size == this.elements.length) {
                this.headIndex = (this.headIndex + 1) % this.elements.length;
            } else {
                this.size++;
            }

            this.tailIndex = (this.tailIndex + 1) % this.elements.length;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * get
     *
     * @param index
     * @return
     */
    public T get(int index) {
        this.lock.lock();
        try {
            if (index < 0 || index >= this.size) {
                throw new IndexOutOfBoundsException("Index " + index + " is out of bounds");
            }

            int i = (this.headIndex + index) % this.elements.length;
            return (T) this.elements[i];
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * size
     *
     * @return
     */
    public int size() {
        this.lock.lock();
        try {
            return this.size;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * capacity
     *
     * @return
     */
    public int capacity() {
        return this.elements.length;
    }

    /**
     * isEmpty
     *
     * @return
     */
    public boolean isEmpty() {
        this.lock.lock();
        try {
            return this.size == 0;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new CircularListIterator();
    }

    private class CircularListIterator implements Iterator<T> {

        private int current;
        private boolean removable;
        private int remaining;

        public CircularListIterator() {
            this.current = ConcurrentCircularList.this.headIndex;
            this.removable = false;
            this.remaining = ConcurrentCircularList.this.size;
        }

        @Override
        public boolean hasNext() {
            return this.remaining > 0;
        }

        @Override
        public T next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }

            T element = (T) ConcurrentCircularList.this.elements[this.current];
            this.removable = true;
            this.current = (this.current + 1) % ConcurrentCircularList.this.elements.length;
            this.remaining--;

            return element;
        }

        @Override
        public void remove() {
            if (!this.removable) {
                throw new IllegalStateException();
            }

            int deleteIndex = (this.current - 1 + ConcurrentCircularList.this.elements.length) % ConcurrentCircularList.this.elements.length;
            this.current = (this.current - 1 + ConcurrentCircularList.this.elements.length) % ConcurrentCircularList.this.elements.length;
            ConcurrentCircularList.this.elements[deleteIndex] = null;
            ConcurrentCircularList.this.headIndex = this.current;
            ConcurrentCircularList.this.size--;
            this.remaining--;
            this.removable = false;
        }
    }
}

