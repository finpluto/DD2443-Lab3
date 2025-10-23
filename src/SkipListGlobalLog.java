public class SkipListGlobalLog<T extends Comparable<T>> extends SkipListLocked<T> {
    @Override
    protected boolean needLockingAtLP() {
        return false;
    }
}
