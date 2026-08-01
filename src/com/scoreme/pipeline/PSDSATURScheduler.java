package com.scoreme.pipeline;

import java.util.*;

/**
 * Priority-SLA-Resource DSATUR with Multi-Restart & Local Search (PS-DSATUR-LS).
 * Primary original heuristic solver for MSME Credit Pipeline Scheduling.
 */
public class PSDSATURScheduler {

    private final Models.ProblemInstance instance;
    private final int maxLocalSearchIters;
    private final int numRestarts;

    public PSDSATURScheduler(Models.ProblemInstance instance) {
        this(instance, 1000, 50);
    }

    public PSDSATURScheduler(Models.ProblemInstance instance, int maxLocalSearchIters, int numRestarts) {
        this.instance = instance;
        this.maxLocalSearchIters = maxLocalSearchIters;
        this.numRestarts = numRestarts;
    }

    public Models.ScheduleResult solve() {
        long startTime = System.currentTimeMillis();
        int n = instance.tasks().size();
        int K = instance.K();

        int[] bestOverallAssignment = null;
        double bestOverallPenalty = Double.MAX_VALUE;
        String lastViolationReason = "No feasible slot assignment could be found under current constraints.";

        // Multi-Restart Strategy to explore search space
        for (int restart = 0; restart < numRestarts; restart++) {
            int[] assignment = new int[n];
            Arrays.fill(assignment, -1);
            double[][] currentSlotUsage = new double[K][instance.d()];

            Random rng = (restart == 0) ? null : new Random(42 + restart);
            boolean success = constructSchedule(assignment, currentSlotUsage, rng);

            if (success) {
                FeasibilityChecker.FeasibilityResult check = FeasibilityChecker.checkFeasibility(instance, assignment);
                if (check.isFeasible()) {
                    // Local Search Optimization
                    assignment = optimizeLocalSearch(assignment, currentSlotUsage, rng != null ? rng : new Random(42));
                    double penalty = PenaltyFunction.calculatePenalty(instance, assignment);
                    if (penalty < bestOverallPenalty) {
                        bestOverallPenalty = penalty;
                        bestOverallAssignment = assignment.clone();
                    }
                }
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        if (bestOverallAssignment == null) {
            return new Models.ScheduleResult(null, -1.0, totalTime, false, lastViolationReason);
        }

        Map<String, Integer> assignmentMap = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            assignmentMap.put(instance.tasks().get(i).id(), bestOverallAssignment[i]);
        }

        return new Models.ScheduleResult(assignmentMap, bestOverallPenalty, totalTime, true, null);
    }

    private boolean constructSchedule(int[] assignment, double[][] slotUsage, Random rng) {
        int n = instance.tasks().size();
        boolean[] assigned = new boolean[n];

        for (int step = 0; step < n; step++) {
            int bestTaskIdx = selectNextTask(assigned, assignment, slotUsage, rng);
            if (bestTaskIdx == -1) return false;

            Models.Task task = instance.tasks().get(bestTaskIdx);
            int chosenSlot = selectBestSlotForTask(bestTaskIdx, assignment, slotUsage);

            if (chosenSlot == -1) {
                chosenSlot = tryBacktrackAssignment(bestTaskIdx, assigned, assignment, slotUsage);
                if (chosenSlot == -1) return false;
            }

            assignment[bestTaskIdx] = chosenSlot;
            assigned[bestTaskIdx] = true;
            for (int dim = 0; dim < instance.d(); dim++) {
                slotUsage[chosenSlot][dim] += task.resources()[dim];
            }
        }
        return true;
    }

    private int selectNextTask(boolean[] assigned, int[] assignment, double[][] slotUsage, Random rng) {
        int n = instance.tasks().size();
        int bestIdx = -1;
        double maxScore = -Double.MAX_VALUE;

        for (int i = 0; i < n; i++) {
            if (assigned[i]) continue;

            Models.Task task = instance.tasks().get(i);
            int saturation = calculateSaturationDegree(i, assignment, slotUsage);
            double windowSpan = task.windowEnd() - task.windowStart() + 1.0;
            double weight = task.weight();

            // Compound priority score
            double score = (saturation * 100.0) + (weight * 10.0) - (windowSpan * 5.0);
            if (rng != null) {
                score += rng.nextDouble() * 20.0; // Random noise for multi-start exploration
            }

            if (score > maxScore) {
                maxScore = score;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private int calculateSaturationDegree(int taskIdx, int[] assignment, double[][] slotUsage) {
        Models.Task task = instance.tasks().get(taskIdx);
        int unavailableSlots = 0;

        for (int s = task.windowStart(); s <= task.windowEnd(); s++) {
            if (!isSlotFeasible(taskIdx, s, assignment, slotUsage)) {
                unavailableSlots++;
            }
        }
        return unavailableSlots;
    }

    private boolean isSlotFeasible(int taskIdx, int slot, int[] assignment, double[][] slotUsage) {
        Models.Task task = instance.tasks().get(taskIdx);

        if (slot < task.windowStart() || slot > task.windowEnd()) return false;

        for (int neighbor : instance.conflictGraph().getNeighbors(taskIdx)) {
            if (assignment[neighbor] == slot) return false;
        }

        double[] cap = instance.slotCapacities().get(slot);
        for (int dim = 0; dim < instance.d(); dim++) {
            if (slotUsage[slot][dim] + task.resources()[dim] > cap[dim] + 1e-6) {
                return false;
            }
        }

        return true;
    }

    private int selectBestSlotForTask(int taskIdx, int[] assignment, double[][] slotUsage) {
        Models.Task task = instance.tasks().get(taskIdx);
        int bestSlot = -1;
        double minPenaltyDelta = Double.MAX_VALUE;

        for (int s = task.windowStart(); s <= task.windowEnd(); s++) {
            if (isSlotFeasible(taskIdx, s, assignment, slotUsage)) {
                assignment[taskIdx] = s;
                double penalty = PenaltyFunction.calculatePenalty(instance, assignment);
                assignment[taskIdx] = -1;

                if (penalty < minPenaltyDelta) {
                    minPenaltyDelta = penalty;
                    bestSlot = s;
                }
            }
        }
        return bestSlot;
    }

    private int tryBacktrackAssignment(int taskIdx, boolean[] assigned, int[] assignment, double[][] slotUsage) {
        Models.Task task = instance.tasks().get(taskIdx);
        int n = instance.tasks().size();

        for (int s = task.windowStart(); s <= task.windowEnd(); s++) {
            List<Integer> blockingTasks = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (assigned[i] && assignment[i] == s) {
                    blockingTasks.add(i);
                }
            }

            for (int blockerIdx : blockingTasks) {
                int oldSlot = assignment[blockerIdx];
                Models.Task blockerTask = instance.tasks().get(blockerIdx);

                for (int dim = 0; dim < instance.d(); dim++) {
                    slotUsage[oldSlot][dim] -= blockerTask.resources()[dim];
                }
                assignment[blockerIdx] = -1;

                if (isSlotFeasible(taskIdx, s, assignment, slotUsage)) {
                    int newBlockerSlot = selectBestSlotForTask(blockerIdx, assignment, slotUsage);
                    if (newBlockerSlot != -1) {
                        assignment[blockerIdx] = newBlockerSlot;
                        for (int dim = 0; dim < instance.d(); dim++) {
                            slotUsage[newBlockerSlot][dim] += blockerTask.resources()[dim];
                        }
                        return s;
                    }
                }

                assignment[blockerIdx] = oldSlot;
                for (int dim = 0; dim < instance.d(); dim++) {
                    slotUsage[oldSlot][dim] += blockerTask.resources()[dim];
                }
            }
        }
        return -1;
    }

    private int[] optimizeLocalSearch(int[] initialAssignment, double[][] slotUsage, Random random) {
        int n = instance.tasks().size();
        int[] bestAssignment = initialAssignment.clone();
        double bestPenalty = PenaltyFunction.calculatePenalty(instance, bestAssignment);

        int[] currentAssignment = initialAssignment.clone();

        for (int iter = 0; iter < maxLocalSearchIters; iter++) {
            boolean improved = false;

            int taskIdx = random.nextInt(n);
            int oldSlot = currentAssignment[taskIdx];
            Models.Task task = instance.tasks().get(taskIdx);

            for (int dim = 0; dim < instance.d(); dim++) {
                slotUsage[oldSlot][dim] -= task.resources()[dim];
            }
            currentAssignment[taskIdx] = -1;

            int targetSlot = task.windowStart() + random.nextInt(task.windowEnd() - task.windowStart() + 1);
            if (isSlotFeasible(taskIdx, targetSlot, currentAssignment, slotUsage)) {
                currentAssignment[taskIdx] = targetSlot;
                for (int dim = 0; dim < instance.d(); dim++) {
                    slotUsage[targetSlot][dim] += task.resources()[dim];
                }

                double candidatePenalty = PenaltyFunction.calculatePenalty(instance, currentAssignment);
                if (candidatePenalty < bestPenalty) {
                    bestPenalty = candidatePenalty;
                    bestAssignment = currentAssignment.clone();
                    improved = true;
                }
            } else {
                currentAssignment[taskIdx] = oldSlot;
                for (int dim = 0; dim < instance.d(); dim++) {
                    slotUsage[oldSlot][dim] += task.resources()[dim];
                }
            }

            int u = random.nextInt(n);
            int v = random.nextInt(n);
            if (u != v && currentAssignment[u] != currentAssignment[v]) {
                int slotU = currentAssignment[u];
                int slotV = currentAssignment[v];

                Models.Task taskU = instance.tasks().get(u);
                Models.Task taskV = instance.tasks().get(v);

                currentAssignment[u] = -1;
                currentAssignment[v] = -1;
                for (int dim = 0; dim < instance.d(); dim++) {
                    slotUsage[slotU][dim] -= taskU.resources()[dim];
                    slotUsage[slotV][dim] -= taskV.resources()[dim];
                }

                if (isSlotFeasible(u, slotV, currentAssignment, slotUsage) &&
                    isSlotFeasible(v, slotU, currentAssignment, slotUsage)) {
                    currentAssignment[u] = slotV;
                    currentAssignment[v] = slotU;
                    for (int dim = 0; dim < instance.d(); dim++) {
                        slotUsage[slotV][dim] += taskU.resources()[dim];
                        slotUsage[slotU][dim] += taskV.resources()[dim];
                    }

                    double swapPenalty = PenaltyFunction.calculatePenalty(instance, currentAssignment);
                    if (swapPenalty < bestPenalty) {
                        bestPenalty = swapPenalty;
                        bestAssignment = currentAssignment.clone();
                        improved = true;
                    } else if (!improved && random.nextDouble() > 0.95) {
                        // Accept minor worsening step to escape local optimum
                    } else {
                        currentAssignment[u] = slotU;
                        currentAssignment[v] = slotV;
                        for (int dim = 0; dim < instance.d(); dim++) {
                            slotUsage[slotV][dim] -= taskU.resources()[dim];
                            slotUsage[slotU][dim] -= taskV.resources()[dim];
                            slotUsage[slotU][dim] += taskU.resources()[dim];
                            slotUsage[slotV][dim] += taskV.resources()[dim];
                        }
                    }
                } else {
                    currentAssignment[u] = slotU;
                    currentAssignment[v] = slotV;
                    for (int dim = 0; dim < instance.d(); dim++) {
                        slotUsage[slotU][dim] += taskU.resources()[dim];
                        slotUsage[slotV][dim] += taskV.resources()[dim];
                    }
                }
            }
        }

        return bestAssignment;
    }
}
