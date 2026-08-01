package com.scoreme.pipeline;

import java.util.*;

/**
 * Validates Feasibility constraints F1, F2, F3 for a given assignment.
 */
public class FeasibilityChecker {

    public record FeasibilityResult(boolean isFeasible, String violationReason) {}

    public static FeasibilityResult checkFeasibility(Models.ProblemInstance instance, int[] assignment) {
        int n = instance.tasks().size();
        int K = instance.K();
        int d = instance.d();

        // Check complete assignment
        for (int i = 0; i < n; i++) {
            if (assignment[i] < 0) {
                return new FeasibilityResult(false, "Unassigned task: " + instance.tasks().get(i).id());
            }
        }

        // F3. SLA Window Bounds
        for (int i = 0; i < n; i++) {
            Models.Task task = instance.tasks().get(i);
            int slot = assignment[i];
            if (slot < task.windowStart() || slot > task.windowEnd()) {
                return new FeasibilityResult(false, String.format(
                    "SLA Breach F3: Task %s assigned to slot %d outside window [%d, %d]",
                    task.id(), slot, task.windowStart(), task.windowEnd()
                ));
            }
        }

        // F1. Conflict Avoidance
        Models.ConflictGraph graph = instance.conflictGraph();
        for (int u = 0; u < n; u++) {
            for (int v = u + 1; v < n; v++) {
                if (graph.hasConflict(u, v) && assignment[u] == assignment[v]) {
                    return new FeasibilityResult(false, String.format(
                        "Conflict Violation F1: Tasks %s and %s share slot %d",
                        instance.tasks().get(u).id(), instance.tasks().get(v).id(), assignment[u]
                    ));
                }
            }
        }

        // F2. Resource Capacity Constraints
        double[][] slotUsage = new double[K][d];
        for (int i = 0; i < n; i++) {
            int slot = assignment[i];
            double[] res = instance.tasks().get(i).resources();
            for (int dim = 0; dim < d; dim++) {
                slotUsage[slot][dim] += res[dim];
            }
        }

        for (int s = 0; s < K; s++) {
            double[] cap = instance.slotCapacities().get(s);
            for (int dim = 0; dim < d; dim++) {
                if (slotUsage[s][dim] > cap[dim] + 1e-6) {
                    return new FeasibilityResult(false, String.format(
                        "Capacity Exceeded F2: Slot %d dimension %d usage %.2f > capacity %.2f",
                        s, dim, slotUsage[s][dim], cap[dim]
                    ));
                }
            }
        }

        return new FeasibilityResult(true, null);
    }
}
