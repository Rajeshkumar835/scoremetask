package com.scoreme.pipeline;

import java.util.*;

/**
 * Domain Models using Java 17 Records and classes.
 */
public class Models {

    /**
     * Represents a single pipeline task.
     */
    public record Task(
        String id,
        int index,
        double weight,
        int windowStart,
        int windowEnd,
        double[] resources
    ) {
        @Override
        public String toString() {
            return String.format("%s[win=[%d,%d], w=%.2f, res=%s]", 
                id, windowStart, windowEnd, weight, Arrays.toString(resources));
        }
    }

    /**
     * Represents a processing slot's multi-dimensional capacity.
     */
    public record SlotCapacity(
        int slotIndex,
        double[] capacities
    ) {}

    /**
     * Conflict Graph representation.
     */
    public static class ConflictGraph {
        private final int n;
        private final boolean[][] adjMatrix;
        private final Set<Integer>[] adjList;

        @SuppressWarnings("unchecked")
        public ConflictGraph(int n, List<int[]> edgeList) {
            this.n = n;
            this.adjMatrix = new boolean[n][n];
            this.adjList = new Set[n];
            for (int i = 0; i < n; i++) {
                adjList[i] = new HashSet<>();
            }
            for (int[] edge : edgeList) {
                int u = edge[0];
                int v = edge[1];
                if (u >= 0 && u < n && v >= 0 && v < n && u != v) {
                    adjMatrix[u][v] = true;
                    adjMatrix[v][u] = true;
                    adjList[u].add(v);
                    adjList[v].add(u);
                }
            }
        }

        public boolean hasConflict(int u, int v) {
            return adjMatrix[u][v];
        }

        public Set<Integer> getNeighbors(int u) {
            return adjList[u];
        }

        public int getDegree(int u) {
            return adjList[u].size();
        }

        public int size() {
            return n;
        }
    }

    /**
     * Complete problem instance description.
     */
    public record ProblemInstance(
        List<Task> tasks,
        ConflictGraph conflictGraph,
        List<double[]> slotCapacities,
        int K,
        int d
    ) {}

    /**
     * Scheduling Output Result conforming to Section 5 specification.
     */
    public record ScheduleResult(
        Map<String, Integer> assignment,
        double penalty,
        long runtimeMs,
        boolean feasible,
        String violationReason
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("assignment", assignment != null ? assignment : new HashMap<>());
            map.put("penalty", penalty);
            map.put("runtime_ms", runtimeMs);
            map.put("feasible", feasible);
            map.put("violation_reason", violationReason != null ? violationReason : "");
            return map;
        }
    }
}
