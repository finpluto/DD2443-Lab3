import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class LockFreeSkipList<T extends Comparable<T>> implements LockFreeSet<T> {
        /* Number of levels */
        private static final int MAX_LEVEL = 16;

        private final Node<T> head = new Node<T>();
        private final Node<T> tail = new Node<T>();

        private ReentrantLock lock = new ReentrantLock();

        public LockFreeSkipList() {
                for (int i = 0; i < head.next.length; i++) {
                        head.next[i] = new AtomicMarkableReference<LockFreeSkipList.Node<T>>(tail, false);
                }
        }

        private static final class Node<T> {
                private final T value;
                private final AtomicMarkableReference<Node<T>>[] next;
                private final int topLevel;

                @SuppressWarnings("unchecked")
                public Node() {
                        value = null;
                        next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[MAX_LEVEL + 1];
                        for (int i = 0; i < next.length; i++) {
                                next[i] = new AtomicMarkableReference<Node<T>>(null, false);
                        }
                        topLevel = MAX_LEVEL;
                }

                @SuppressWarnings("unchecked")
                public Node(T x, int height) {
                        value = x;
                        next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[height + 1];
                        for (int i = 0; i < next.length; i++) {
                                next[i] = new AtomicMarkableReference<Node<T>>(null, false);
                        }
                        topLevel = height;
                }
        }

        /*
         * Returns a level between 0 to MAX_LEVEL,
         * P[randomLevel() = x] = 1/2^(x+1), for x < MAX_LEVEL.
         */
        private static int randomLevel() {
                int r = ThreadLocalRandom.current().nextInt();
                int level = 0;
                r &= (1 << MAX_LEVEL) - 1;
                while ((r & 1) != 0) {
                        r >>>= 1;
                        level++;
                }
                return level;
        }

        @SuppressWarnings("unchecked")
        public boolean add(int threadId, T x) {
                int topLevel = randomLevel();
                int bottomLevel = 0;
                Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
                Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
                long lpTime = -1;
                while (true) {
                        boolean found = find(x, preds, succs, Caller.ADD);
                        if (found) {
                                return false;
                        } else {
                                Node<T> newNode = new Node<>(x, topLevel);
                                for (int level = bottomLevel; level <= topLevel; level++) {
                                        Node<T> succ = succs[level];
                                        newNode.next[level].set(succ, false);
                                }
                                Node<T> pred = preds[bottomLevel];
                                Node<T> succ = succs[bottomLevel];

                                // only the node linked into bottomlevel list is considering "in" the set, this
                                // is the successful LP of add
                                if (needLockingAtLP())
                                        lock.lock();
                                if (!pred.next[bottomLevel].compareAndSet(succ, newNode, false, false)) {
                                        if (needLockingAtLP())
                                                lock.unlock();
                                        continue;
                                } else {
                                        lpTime = System.nanoTime();
                                        if (needLockingAtLP())
                                                lock.unlock();
                                }
                                for (int level = bottomLevel + 1; level <= topLevel; level++) {
                                        while (true) {
                                                pred = preds[level];
                                                succ = succs[level];
                                                if (pred.next[level].compareAndSet(succ, newNode, false, false))
                                                        break;
                                                find(x, preds, succs, Caller.NOTREPORT);
                                        }
                                }
                                putLog(Log.Method.ADD, x, true, lpTime);
                                return true;
                        }
                }
        }

        @SuppressWarnings("unchecked")
        public boolean remove(int threadId, T x) {
                int bottomLevel = 0;
                Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
                Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
                Node<T> succ;
                long lpTime = -1;
                while (true) {
                        boolean found = find(x, preds, succs, Caller.REMOVE);
                        if (!found) {
                                return false;
                        } else {
                                Node<T> nodeToRemove = succs[bottomLevel];
                                for (int level = nodeToRemove.topLevel; level >= bottomLevel + 1; level--) {
                                        boolean[] marked = { false };
                                        succ = nodeToRemove.next[level].get(marked);
                                        while (!marked[0]) {
                                                nodeToRemove.next[level].compareAndSet(succ, succ, false, true);
                                                succ = nodeToRemove.next[level].get(marked);
                                        }
                                }
                                boolean[] marked = { false };
                                succ = nodeToRemove.next[bottomLevel].get(marked);
                                while (true) {

                                        if (needLockingAtLP())
                                                lock.lock();
                                        boolean iMarkedIt = nodeToRemove.next[bottomLevel].compareAndSet(succ, succ,
                                                        false, true);
                                        // marked by this thread, here is the successful LP of remove
                                        lpTime = System.nanoTime();
                                        if (needLockingAtLP())
                                                lock.unlock();

                                        succ = succs[bottomLevel].next[bottomLevel].get(marked);
                                        if (iMarkedIt) {
                                                find(x, preds, succs, Caller.NOTREPORT);
                                                putLog(Log.Method.REMOVE, x, true, lpTime);
                                                return true;
                                        } else if (marked[0]) { // marked by other thread, the removal is unsuccessful
                                                                // here.
                                                // the LP of this unsuccessful removal is the LP of successfully removal
                                                // in the other thread
                                                return false;
                                        } // the next has changed, retry removing
                                }
                        }
                }
        }

        public boolean contains(int threadId, T x) {
                int bottomLevel = 0;
                boolean[] marked = { false };
                Node<T> pred = head;
                Node<T> curr = null;
                Node<T> succ = null;
                long lpTime = -1;
                for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
                        // first LP
                        if (needLockingAtLP())
                                lock.lock();
                        curr = pred.next[level].getReference();
                        lpTime = System.nanoTime();
                        if (needLockingAtLP())
                                lock.unlock();
                        while (true) {
                                succ = curr.next[level].get(marked);

                                while (marked[0]) {
                                        // LP in the inner most loop
                                        if (needLockingAtLP())
                                                lock.lock();
                                        curr = succ;
                                        lpTime = System.nanoTime();
                                        if (needLockingAtLP())
                                                lock.unlock();

                                        succ = curr.next[level].get(marked);
                                }
                                if (curr.value != null && x.compareTo(curr.value) < 0) {
                                        pred = curr;
                                        curr = succ;
                                } else {
                                        break;
                                }
                        }
                }
                boolean contained = curr.value != null && x.compareTo(curr.value) == 0;
                putLog(Log.Method.CONTAINS, x, contained, lpTime);
                return contained;
        }

        private enum Caller {
                ADD, REMOVE, NOTREPORT
        }

        private boolean find(T x, Node<T>[] preds, Node<T>[] succs, Caller from) {
                int bottomLevel = 0;
                boolean[] marked = { false };
                boolean snip;
                Node<T> pred = null;
                Node<T> curr = null;
                Node<T> succ = null;
                long lpTime = -1;
                retry: while (true) {
                        pred = head;
                        for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
                                // first LP
                                if (needLockingAtLP())
                                        lock.lock();
                                curr = pred.next[level].getReference();
                                lpTime = System.nanoTime();
                                if (needLockingAtLP())
                                        lock.unlock();

                                while (true) {
                                        succ = curr.next[level].get(marked);
                                        while (marked[0]) {
                                                // second LP
                                                if (needLockingAtLP())
                                                        lock.lock();
                                                snip = pred.next[level].compareAndSet(curr, succ, false, false);
                                                if (!snip) {
                                                        continue retry;
                                                }
                                                if (needLockingAtLP())
                                                        lock.unlock();
                                                curr = succ;
                                                lpTime = System.nanoTime();
                                                if (needLockingAtLP())
                                                        lock.unlock();
                                                succ = curr.next[level].get(marked);
                                        }
                                        if (curr.value != null && x.compareTo(curr.value) < 0) {
                                                pred = curr;
                                                curr = succ;
                                        } else {
                                                break;
                                        }
                                }

                                preds[level] = pred;
                                succs[level] = curr;
                        }

                        // report lp time and action to logger
                        boolean found = curr.value != null && x.compareTo(curr.value) == 0;
                        if (found && from == Caller.ADD) { // found, means add failed
                                putLog(Log.Method.ADD, x, false, lpTime);
                        }
                        if (!found && from == Caller.REMOVE) { // not found, means remove failed
                                putLog(Log.Method.REMOVE, x, false, lpTime);
                        }
                        return found;
                }
        }

        public ArrayList<Log.Entry<T>> getLog() {
                // This should fetch the log from the skiplist.
                return null;
        }

        protected void putLog(Log.Entry<T> logEntry) {
                return;
        }

        private void putLog(Log.Method method, T arg, boolean ret, long timestamp) {
                putLog(new Log.Entry<T>(method, arg, ret, timestamp));
        }

        protected boolean needLockingAtLP() {
                return false;
        }

        public void reset() {
                for (int i = 0; i < head.next.length; i++) {
                        head.next[i] = new AtomicMarkableReference<LockFreeSkipList.Node<T>>(tail, false);
                }
        }
}
