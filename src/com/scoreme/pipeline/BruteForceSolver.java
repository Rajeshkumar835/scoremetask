package com.scoreme.pipeline;

import java.util.*;

/**
 * Exact Brute-Force Solver for small instances (n <= 12).
 * Used to compute exact optimal solution P(sigma*) for benchmarking approximation ratio.
 */
public class BruteForceSolver {

    private final Models.ProblemInstance instance;
    private double minPenalty = Double.MAX_VALUE;
    private int[] bestAssignment = null;
    private boolean foundFeasible = false;

    public BruteForceSolver(Models.ProblemInstance instance) {
        this.instance = instance;
    }

    public Models.ScheduleResult solve() {
        long startTime = System.currentTimeMillis();
        int n = instance.tasks().size();
        int[] currentAssignment = new int[n];
        double[][] slotUsage = new double[instance.K()][instance.d()];

        search(0, currentAssignment, slotUsage);

        long runtimeMs = System.currentTimeMillis() - startTime;

        if (!foundFeasible || bestAssignment == null) {
            return new Models.ScheduleResult(null, -1.0, runtimeMs, false, "No feasible assignment exists in search space");
        }

        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            map.put(instance.tasks().get(i).id(), bestAssignment[i]);
        }

        return new Models.ScheduleResult(map, minPenalty, runtimeMs, true, null);
    }

    private void search(int taskIdx, int[] assignment, double[][] slotUsage) {
        int n = instance.tasks().size();

        if (taskIdx == n) {
            FeasibilityChecker.FeasibilityResult check = FeasibilityChecker.checkFeasibility(instance, assignment);
            if (check.isFeasible()) {
                double penalty = PenaltyFunction.calculatePenalty(instance, assignment);
                if (penalty < minPenalty) {
                    minPenalty = penalty;
                    bestAssignment = assignment.clone();
                    foundFeasible = true;
                }
            }
            return;
        }

        Models.Task task = instance.tasks().get(taskIdx);
        for (int slot = task.windowStart(); slot <= task.windowEnd(); slot++) {
            if (isFeasiblePartial(taskIdx, slot, assignment, slotUsage)) {
                assignment[taskIdx] = slot;
                for (int dim = 0; dim < instance.d(); dim++) {
                    slotUsage[slot][dim] += task.resources()[dim];
                }

                search(taskIdx + 1, assignment, slotUsage);

                // Backtrack
                for (int dim = 0; dim < instance.d(); dim++) {
                    slotUsage[slot][dim] -= task.resources()[dim];
                }
                assignment[taskIdx] = -1;
            }
        }
    }

    private boolean isFeasiblePartial(int taskIdx, int slot, int[] assignment, double[][] slotUsage) {
        Models.Task task = instance.tasks().get(taskIdx);

        // F1 Conflict check with previously assigned tasks
        for (int neighbor : instance.conflictGraph().getNeighbors(taskIdx)) {
            if (neighbor < taskIdx && assignment[neighbor] == slot) {
                return false;
            }
        }

        // F2 Capacity check
        double[] cap = instance.slotCapacities().get(slot);
        for (int dim = 0; dim < instance.d(); dim++) {
            if (slotUsage[slot][dim] + task.resources()[dim] > cap[dim] + 1e-6) {
                return false;
            }
        }

        return true;
    }
}
