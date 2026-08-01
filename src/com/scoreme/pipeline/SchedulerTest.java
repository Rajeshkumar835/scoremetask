package com.scoreme.pipeline;

import java.util.*;

/**
 * Unit Test Suite for Task 5 requirements.
 * Verifies 4 mandatory edge cases:
 * 1. All-conflict graph (chromatic number > K)
 * 2. Zero-capacity slot
 * 3. Tight SLA windows
 * 4. Single-task instance
 */
public class SchedulerTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   MSME Pipeline Scheduler - Unit Test Runner    ");
        System.out.println("=================================================");

        int passed = 0;
        int total = 4;

        if (testAllConflictGraph()) passed++;
        if (testZeroCapacitySlot()) passed++;
        if (testTightSLAWindows()) passed++;
        if (testSingleTaskInstance()) passed++;

        System.out.println("-------------------------------------------------");
        System.out.printf("Test Summary: %d/%d Passed.\n", passed, total);
        System.out.println("=================================================");

        if (passed != total) {
            System.exit(1);
        }
    }

    private static boolean testAllConflictGraph() {
        System.out.print("[TEST 1/4] All-Conflict Graph (Clique > K) ... ");
        int n = 5;
        int K = 3; // Chromatic number is 5 > 3
        int d = 4;

        List<Models.Task> tasks = new ArrayList<>();
        List<int[]> conflicts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            tasks.add(new Models.Task("T" + i, i, 5.0, 0, K - 1, new double[]{1, 1, 1, 1}));
            for (int j = i + 1; j < n; j++) {
                conflicts.add(new int[]{i, j});
            }
        }

        List<double[]> caps = List.of(
            new double[]{10, 10, 10, 10},
            new double[]{10, 10, 10, 10},
            new double[]{10, 10, 10, 10}
        );

        Models.ConflictGraph graph = new Models.ConflictGraph(n, conflicts);
        Models.ProblemInstance instance = new Models.ProblemInstance(tasks, graph, caps, K, d);

        PSDSATURScheduler scheduler = new PSDSATURScheduler(instance);
        Models.ScheduleResult result = scheduler.solve();

        if (!result.feasible()) {
            System.out.println("PASSED (Correctly detected infeasibility: " + result.violationReason() + ")");
            return true;
        } else {
            System.out.println("FAILED (Should have been infeasible)");
            return false;
        }
    }

    private static boolean testZeroCapacitySlot() {
        System.out.print("[TEST 2/4] Zero-Capacity Slot ... ");
        int n = 2;
        int K = 2;
        int d = 4;

        List<Models.Task> tasks = List.of(
            new Models.Task("T0", 0, 5.0, 0, 1, new double[]{2, 2, 2, 2}),
            new Models.Task("T1", 1, 3.0, 0, 1, new double[]{2, 2, 2, 2})
        );
        // Slot 0 has 0 capacity across all dimensions
        List<double[]> caps = List.of(
            new double[]{0, 0, 0, 0},
            new double[]{10, 10, 10, 10}
        );

        Models.ConflictGraph graph = new Models.ConflictGraph(n, List.of());
        Models.ProblemInstance instance = new Models.ProblemInstance(tasks, graph, caps, K, d);

        PSDSATURScheduler scheduler = new PSDSATURScheduler(instance);
        Models.ScheduleResult result = scheduler.solve();

        if (result.feasible() && result.assignment().get("T0") == 1 && result.assignment().get("T1") == 1) {
            System.out.println("PASSED (Correctly routed all tasks to valid Slot 1)");
            return true;
        } else {
            System.out.println("FAILED or Unexpected result: " + result);
            return false;
        }
    }

    private static boolean testTightSLAWindows() {
        System.out.print("[TEST 3/4] Tight SLA Windows ... ");
        int n = 3;
        int K = 3;
        int d = 4;

        List<Models.Task> tasks = List.of(
            new Models.Task("T0", 0, 5.0, 0, 0, new double[]{2, 2, 2, 2}), // Must be slot 0
            new Models.Task("T1", 1, 5.0, 1, 1, new double[]{2, 2, 2, 2}), // Must be slot 1
            new Models.Task("T2", 2, 5.0, 2, 2, new double[]{2, 2, 2, 2})  // Must be slot 2
        );

        List<double[]> caps = List.of(
            new double[]{10, 10, 10, 10},
            new double[]{10, 10, 10, 10},
            new double[]{10, 10, 10, 10}
        );

        Models.ConflictGraph graph = new Models.ConflictGraph(n, List.of());
        Models.ProblemInstance instance = new Models.ProblemInstance(tasks, graph, caps, K, d);

        PSDSATURScheduler scheduler = new PSDSATURScheduler(instance);
        Models.ScheduleResult result = scheduler.solve();

        if (result.feasible() && 
            result.assignment().get("T0") == 0 &&
            result.assignment().get("T1") == 1 &&
            result.assignment().get("T2") == 2) {
            System.out.println("PASSED (Strict SLA bounds respected)");
            return true;
        } else {
            System.out.println("FAILED (SLA bounds violated)");
            return false;
        }
    }

    private static boolean testSingleTaskInstance() {
        System.out.print("[TEST 4/4] Single Task Instance ... ");
        int n = 1;
        int K = 2;
        int d = 4;

        List<Models.Task> tasks = List.of(
            new Models.Task("T0", 0, 5.0, 0, 1, new double[]{4, 16, 2, 1.0})
        );
        List<double[]> caps = List.of(
            new double[]{32, 128, 8, 6.0},
            new double[]{32, 128, 8, 6.0}
        );

        Models.ConflictGraph graph = new Models.ConflictGraph(n, List.of());
        Models.ProblemInstance instance = new Models.ProblemInstance(tasks, graph, caps, K, d);

        PSDSATURScheduler scheduler = new PSDSATURScheduler(instance);
        Models.ScheduleResult result = scheduler.solve();

        if (result.feasible() && result.assignment().containsKey("T0")) {
            System.out.println("PASSED (Assigned to slot " + result.assignment().get("T0") + " with penalty " + result.penalty() + ")");
            return true;
        } else {
            System.out.println("FAILED");
            return false;
        }
    }
}
