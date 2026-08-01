package com.scoreme.pipeline;

import java.util.*;

/**
 * Task 6: Empirical Benchmarking Runner.
 * Evaluates the 9 Section 4.6 benchmark instances and generates a Markdown table.
 */
public class BenchmarkRunner {

    public record BenchmarkConfig(
        String name,
        int n,
        int K,
        double density,
        long seed,
        boolean runBruteForce
    ) {}

    public static void main(String[] args) {
        System.out.println("==========================================================================================================");
        System.out.println("                         ScoreMe MSME Pipeline Scheduler Benchmark Suite                          ");
        System.out.println("==========================================================================================================");

        List<BenchmarkConfig> configs = List.of(
            // Small instances (compare against brute-force optimal)
            new BenchmarkConfig("Small 1", 8, 3, 0.30, 1, true),
            new BenchmarkConfig("Small 2", 10, 4, 0.40, 2, true),
            new BenchmarkConfig("Small 3", 12, 4, 0.50, 3, true),

            // Medium instances
            new BenchmarkConfig("Medium 1", 50, 8, 0.25, 10, false),
            new BenchmarkConfig("Medium 2", 100, 10, 0.30, 11, false),
            new BenchmarkConfig("Medium 3", 150, 12, 0.35, 12, false),

            // Stress instances
            new BenchmarkConfig("Stress 1", 200, 15, 0.40, 20, false),
            new BenchmarkConfig("Stress 2 (Tight K)", 200, 5, 0.60, 21, false),
            new BenchmarkConfig("Stress 3 (Sparse)", 200, 20, 0.10, 22, false)
        );

        StringBuilder mdTable = new StringBuilder();
        mdTable.append("| Instance | n | K | Density | Feasible | Alg Penalty | Alg Time (ms) | Opt Penalty | Opt Time (ms) | Empirical Ratio (P_alg/P_opt) |\n");
        mdTable.append("| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |\n");

        for (BenchmarkConfig cfg : configs) {
            Models.ProblemInstance instance = InstanceGenerator.generateInstance(cfg.n(), cfg.K(), 4, cfg.density(), cfg.seed());

            // Run PS-DSATUR Scheduler
            PSDSATURScheduler scheduler = new PSDSATURScheduler(instance);
            Models.ScheduleResult algRes = scheduler.solve();

            // Run Brute Force if small
            Models.ScheduleResult optRes = null;
            if (cfg.runBruteForce()) {
                BruteForceSolver brute = new BruteForceSolver(instance);
                optRes = brute.solve();
            }

            String algPenStr = algRes.feasible() ? String.format("%.2f", algRes.penalty()) : "N/A";
            String algTimeStr = String.valueOf(algRes.runtimeMs());
            String optPenStr = (optRes != null && optRes.feasible()) ? String.format("%.2f", optRes.penalty()) : (cfg.runBruteForce() ? "Infeasible" : "N/A");
            String optTimeStr = (optRes != null) ? String.valueOf(optRes.runtimeMs()) : "N/A";

            String ratioStr = "N/A";
            if (algRes.feasible() && optRes != null && optRes.feasible() && optRes.penalty() > 0) {
                double ratio = algRes.penalty() / optRes.penalty();
                ratioStr = String.format("%.4f", ratio);
            }

            String row = String.format(
                "| %-16s | %3d | %2d | %6.2f | %-8s | %11s | %13s | %11s | %13s | %29s |",
                cfg.name(), cfg.n(), cfg.K(), cfg.density(),
                algRes.feasible() ? "YES" : "NO (" + algRes.violationReason() + ")",
                algPenStr, algTimeStr, optPenStr, optTimeStr, ratioStr
            );

            System.out.println(row);
            mdTable.append(row).append("\n");
        }

        System.out.println("==========================================================================================================");
        System.out.println("\nGenerated Markdown Summary Table:\n");
        System.out.println(mdTable.toString());
    }
}
