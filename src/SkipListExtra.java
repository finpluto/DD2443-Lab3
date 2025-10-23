import java.util.ArrayList;
import java.util.LinkedList;

public class SkipListExtra<T extends Comparable<T>> extends LockFreeSkipList<T> {
    LockFreeQueue<Log.Entry<T>> logs = new LockFreeQueue<>();

    @Override
    protected void putLog(Log.Entry<T> entry) {
        logs.enq(entry);
    }

    @Override
    public ArrayList<Log.Entry<T>> getLog() {
        ArrayList<Log.Entry<T>> aggregatedLogs = new ArrayList<>();
        while (true) {
            try {
                aggregatedLogs.add(logs.deq());
            } catch (Exception e) { // while queue is empty
                break;
            }
        }
        return aggregatedLogs;
    }

    @Override
    protected boolean needLockingAtLP() {
        // disable locks in LockFreeSkipList
        return false;
    }

    @Override
    public void reset() {
        super.reset();
        // recreate a new LockFreeQueue
        logs = new LockFreeQueue<>();
    }
}
