import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;

public class Main {

        public static void main(String[] args) {
                // Number of threads to use
                int threads = Integer.parseInt(args[0]);

                // LockFreeSet type to use
                String setName = args[1];

                // Uniform or Normal
                String distributionName = args[2];

                // Max input value
                int maxValue = Integer.parseInt(args[3]);

                // Distribution of adds/removes/contains
                int[] ops = Arrays.stream(args[4].split(":"))
                                .mapToInt(v -> Integer.parseInt(v)).toArray();

                // Number of operations executed per thread.
                int opsPerThread = Integer.parseInt(args[5]);

                // Warm up rounds
                int warmups = Integer.parseInt(args[6]);

                // Measurement rounds
                int measurements = Integer.parseInt(args[7]);

                LockFreeSet<Integer> set = getSet(setName, threads);
                Distribution opsDistribution = new Distribution.Discrete(42, ops);
                Distribution valuesDistribution = getDistribution(distributionName, maxValue);

                if (ops.length != 3) {
                        System.err.println("ERROR: Distribution of operations is " + args[3]);
                        System.err.println("       should be <adds>:<removes>:<contains>.");
                        System.exit(1);
                }
                if (set == null) {
                        System.err.println("ERROR: Unknown set");
                        System.exit(1);
                }
                if (valuesDistribution == null) {
                        System.err.println("ERROR: Unknown distribution");
                        System.exit(1);
                }

                for (int i = 0; i < warmups; ++i) {
                        set.reset();
                        long time = Experiment.run(threads, opsPerThread, set, opsDistribution, valuesDistribution);
                        int discrepancy = Log.validate(set.getLog());
                        System.err.println("Warmup time: " + time);
                        System.err.println("Warmup discrepancy: " + discrepancy);
                }

                for (int i = 0; i < measurements; ++i) {
                        set.reset();
                        long time = Experiment.run(threads, opsPerThread, set, opsDistribution, valuesDistribution);
                        int discrepancy = Log.validate(set.getLog());
                        System.err.println("Measurement time: " + time);
                        System.err.println("Measurement discrepancy: " + discrepancy);
                        // logging for plotting
                        System.out.println(String.format("%s,%s,%d,%d:%d:%d,%d,%d", setName, distributionName,
                                        threads, ops[0], ops[1], ops[2], time, discrepancy));
                }
        }

        public static Distribution getDistribution(String name, int maxValue) {
                switch (name) {
                        case "Uniform":
                                return new Distribution.Uniform(0xdeadbeef, 0, maxValue);
                        case "Normal":
                                return new Distribution.Normal(0xcafecafe, 10, 0, maxValue);
                        default:
                                return null;
                }
        }

        public static LockFreeSet<Integer> getSet(String name, int threads) {
                switch (name) {
                        case "Default":
                                return new LockFreeSkipList<>();
                        case "Locked":
                                return new SkipListLocked<>();
                        case "LocalLog":
                                return new SkipListLocalLog<>();
                        case "GlobalLog":
                                return new SkipListGlobalLog<>();
                        case "Extra":
                                return new SkipListExtra<>();
                        default:
                                return null;
                }
        }
}
