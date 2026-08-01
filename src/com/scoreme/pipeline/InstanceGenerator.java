package com.scoreme.pipeline;

import java.util.*;

/**
 * Instance Generator for MSME Credit Pipeline Scheduling Problem.
 * Exact Java 17 implementation matching Section 5 instance generator specification.
 */
public class InstanceGenerator {

    public static Models.ProblemInstance generateInstance(int n, int K, int d, double conflictDensity, long seed) {
        Random rand = new Random(seed);

        // Tasks
        List<Models.Task> tasks = new ArrayList<>();

        // Conflicts
        List<int[]> conflicts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (rand.nextDouble() < conflictDensity) {
                    conflicts.add(new int[]{i, j});
                }
            }
        }

        double[] capBase = new double[]{32.0, 128.0, 8.0, 6.0};
        List<double[]> slotCapacities = new ArrayList<>();
        for (int s = 0; s < K; s++) {
            slotCapacities.add(capBase.clone());
        }

        int maxDivisor = (n / K) + 1;
        for (int i = 0; i < n; i++) {
            String id = "T" + i;
            double[] res = new double[d];
            for (int dim = 0; dim < d; dim++) {
                double maxVal = capBase[dim] / maxDivisor;
                if (maxVal < 1.0) maxVal = 1.0;
                res[dim] = 1.0 + rand.nextDouble() * (maxVal - 1.0);
            }

            int lo = rand.nextInt(K - 1); // [0, K-2]
            int hi = lo + 1 + rand.nextInt(K - 1 - lo); // [lo+1, K-1]
            double weight = 1.0 + rand.nextDouble() * 9.0; // [1.0, 10.0]

            tasks.add(new Models.Task(id, i, weight, lo, hi, res));
        }

        Models.ConflictGraph graph = new Models.ConflictGraph(n, conflicts);
        return new Models.ProblemInstance(tasks, graph, slotCapacities, K, d);
    }
}
