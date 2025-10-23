import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class SkipListLocalLog<T extends Comparable<T>> extends LockFreeSkipList<T> {
    ConcurrentHashMap<Long, LinkedList<Log.Entry<T>>> logRegistry = new ConcurrentHashMap<>();

    // this threadlocal reference is to avoid accessing logRegistry everytime we
    // putLog.
    ThreadLocal<LinkedList<Log.Entry<T>>> localLogs = ThreadLocal
            .withInitial(() -> {
                long id = Thread.currentThread().getId();
                logRegistry.putIfAbsent(id, new LinkedList<>());
                return logRegistry.get(id);
            });

    @Override
    protected void putLog(Log.Entry<T> entry) {
        localLogs.get().add(entry);
    }

    @Override
    public ArrayList<Log.Entry<T>> getLog() {
        ArrayList<Log.Entry<T>> aggregateLogs = new ArrayList<>();
        for (LinkedList<Log.Entry<T>> logs : logRegistry.values()) {
            aggregateLogs.addAll(logs);
        }
        return aggregateLogs;
    }

    @Override
    protected boolean needLockingAtLP() {
        return false;
    }

    @Override
    public void reset() {
        super.reset();
        // remove all localLog list in registry
        logRegistry.clear();
    }
}
