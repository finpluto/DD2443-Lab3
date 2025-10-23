import java.util.ArrayList;
import java.util.List;

public interface LockFreeSet<T extends Comparable<T>> {
        // Add an element using thread `threadId`.
        boolean add(int threadId, T item);
        // Remove an element using thread `threadId`.
        boolean remove(int threadId, T item);
        // Check if an element is present using thread `threadId`.
        boolean contains(int threadId, T item);
        // Get the log.
        ArrayList<Log.Entry<T>> getLog();
        // Resets the skiplist.
        void reset();
}
