import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SkipListLocked<T extends Comparable<T>> extends LockFreeSkipList<T> {
    // This logs queue is shared in the global log implementation,
    // which use lock-free linked queue from java.util.concurrent
    ConcurrentLinkedQueue<Log.Entry<T>> logs = new ConcurrentLinkedQueue<>();

    @Override
    protected void putLog(Log.Entry<T> entry) {
        logs.add(entry);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ArrayList<Log.Entry<T>> getLog() {
        ArrayList<Log.Entry<T>> aggregatedLogs = new ArrayList<>();
        aggregatedLogs.addAll(logs);
        return aggregatedLogs;
    }

    @Override
    protected boolean needLockingAtLP() {
        // enable locks in LockFreeSkipList
        return true;
    }

    @Override
    public void reset() {
        super.reset();
        // remove all logs
        logs.clear();
    }
}
