package com.scoreme.pipeline;

import java.util.*;

/**
 * Domain-Grounded Penalty Function Implementation (Task 2).
 * Combines:
 * 1. Base delay cost: sum(w_i * slot_i)
 * 2. Load imbalance variance across slots: alpha * sum(||U_s - U_avg||^2)
 * 3. SLA boundary risk penalty: beta * sum(w_i * ((sigma_i - l_i)/(u_i - l_i + eps))^2)
 */
public class PenaltyFunction {

    public static final double ALPHA = 5.0; // Load imbalance weight
    public static final double BETA = 2.0;  // SLA risk weight
    public static final double EPS = 1e-5;

    public static double calculatePenalty(Models.ProblemInstance instance, int[] assignment) {
        int n = instance.tasks().size();
        int K = instance.K();
        int d = instance.d();

        // 1. Base Delay Penalty
        double basePenalty = 0.0;
        for (int i = 0; i < n; i++) {
            if (assignment[i] >= 0) {
                Models.Task task = instance.tasks().get(i);
                basePenalty += task.weight() * assignment[i];
            }
        }

        // 2. Resource Load Imbalance Variance
        double[][] slotUsage = new double[K][d];
        for (int i = 0; i < n; i++) {
            int slot = assignment[i];
            if (slot >= 0 && slot < K) {
                double[] res = instance.tasks().get(i).resources();
                for (int dim = 0; dim < d; dim++) {
                    slotUsage[slot][dim] += res[dim];
                }
            }
        }

        // Calculate utilization matrix U_s,dim = slotUsage / slotCapacity
        double[][] utilization = new double[K][d];
        double[] avgUtil = new double[d];
        for (int s = 0; s < K; s++) {
            double[] cap = instance.slotCapacities().get(s);
            for (int dim = 0; dim < d; dim++) {
                utilization[s][dim] = cap[dim] > 0 ? slotUsage[s][dim] / cap[dim] : 0.0;
                avgUtil[dim] += utilization[s][dim];
            }
        }
        for (int dim = 0; dim < d; dim++) {
            avgUtil[dim] /= K;
        }

        double loadImbalancePenalty = 0.0;
        for (int s = 0; s < K; s++) {
            for (int dim = 0; dim < d; dim++) {
                double diff = utilization[s][dim] - avgUtil[dim];
                loadImbalancePenalty += diff * diff;
            }
        }

        // 3. SLA Boundary Proximity Penalty
        double slaRiskPenalty = 0.0;
        for (int i = 0; i < n; i++) {
            int slot = assignment[i];
            if (slot >= 0) {
                Models.Task task = instance.tasks().get(i);
                double windowSpan = task.windowEnd() - task.windowStart() + EPS;
                double offsetRatio = (slot - task.windowStart()) / windowSpan;
                slaRiskPenalty += task.weight() * (offsetRatio * offsetRatio);
            }
        }

        return basePenalty + (ALPHA * loadImbalancePenalty) + (BETA * slaRiskPenalty);
    }
}
