import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

public class Log {
        private Log() {
                // Do not implement
        }

        public static int validate(ArrayList<Log.Entry<Integer>> log) {
                if (log == null)
                        return -1;
                int discrepancies = 0;

                // sort log entry by timestamp, ascending order
                log.sort(new Comparator<Log.Entry<Integer>>() {
                        @Override
                        public int compare(Log.Entry<Integer> e1, Log.Entry<Integer> e2) {
                                return Long.compare(e1.timestamp, e2.timestamp);
                        }
                });

                HashSet<Integer> testSet = new HashSet<>();
                // replay log records on testSet, in timestamp order
                for (Log.Entry<Integer> entry : log) {
                        // Log.Entry<Integer> entry = log[i];
                        if (entry.timestamp == -1)
                                continue;

                        Boolean ret = null;
                        switch (entry.method) {
                                case ADD:
                                        ret = testSet.add(entry.arg);
                                        break;
                                case REMOVE:
                                        ret = testSet.remove(entry.arg);
                                        break;
                                case CONTAINS:
                                        ret = testSet.contains(entry.arg);
                                        break;
                        }
                        if (ret == null) {
                                // this branch should be unreachable
                                continue;
                        }
                        if (ret != entry.ret) {
                                discrepancies++;
                        }
                }

                return discrepancies;
        }

        // Log entry for linearization point.
        public static class Entry<T extends Comparable<T>> {
                public Method method;
                public T arg;
                public boolean ret;
                public long timestamp;

                public Entry(Method method, T arg, boolean ret, long timestamp) {
                        this.method = method;
                        this.arg = arg;
                        this.ret = ret;
                        this.timestamp = timestamp;
                }
        }

        public static enum Method {
                ADD, REMOVE, CONTAINS
        }
}
