import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Experiment {

        public static long run(int threads, int opsPerThread, LockFreeSet<Integer> list, Distribution ops, Distribution values) {
                ExecutorService executorService = Executors.newFixedThreadPool(threads);

                Task[] tasks = new Task[threads];



                for (int i = 0; i < tasks.length; ++i) {
                        tasks[i] = new Task(i, opsPerThread, list, ops.copy(i), values.copy(-i));
                }

                try {
                        long startTime = System.nanoTime();
                        executorService.invokeAll(Arrays.asList(tasks));
                        long endTime = System.nanoTime();
                        executorService.shutdown();
                        return endTime - startTime;
                } catch (Exception e) {
                        e.printStackTrace();
                }
                return -1;
        }

        public static class Task implements Callable<Void> {
                private final int threadId;
                private final LockFreeSet<Integer> set;
                private final Distribution ops, values;
                private final int opsPerThread;

                public Task(int threadId, int opsPerThread, LockFreeSet<Integer> set, Distribution ops, Distribution values) {
                        this.threadId = threadId;
                        this.set = set;
                        this.ops = ops;
                        this.values = values;
                        this.opsPerThread = opsPerThread;
                }

                public Void call() throws Exception {
                        for (int i = 0; i < opsPerThread; ++i) {
                                int val = values.next();
                                int op = ops.next();
                                switch (op) {
                                        case 0:
                                                set.add(threadId, val);
                                                break;
                                        case 1:
                                                set.remove(threadId, val);
                                                break;
                                        case 2:
                                                set.contains(threadId, val);
                                                break;
                                }
                        }
                        return null;
                }
        }

}
