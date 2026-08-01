package com.scoreme.pipeline;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * CLI Entry Point for MSME Credit Pipeline Scheduler.
 * Usage:
 *   java com.scoreme.pipeline.PipelineScheduler --input instance.json --output result.json
 *   java com.scoreme.pipeline.PipelineScheduler --n 50 --K 8 --density 0.25 --seed 10
 */
public class PipelineScheduler {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        try {
            Map<String, String> argMap = parseArgs(args);

            Models.ProblemInstance instance;

            if (argMap.containsKey("input")) {
                String inputPath = argMap.get("input");
                String content = Files.readString(Path.of(inputPath));
                Map<String, Object> jsonMap = JsonUtils.parseJson(content);
                instance = parseInstanceFromJson(jsonMap);
            } else if (argMap.containsKey("n") && argMap.containsKey("K")) {
                int n = Integer.parseInt(argMap.get("n"));
                int K = Integer.parseInt(argMap.get("K"));
                double density = Double.parseDouble(argMap.getOrDefault("density", "0.3"));
                long seed = Long.parseLong(argMap.getOrDefault("seed", "42"));
                instance = InstanceGenerator.generateInstance(n, K, 4, density, seed);
            } else {
                System.out.println("Usage:");
                System.out.println("  java com.scoreme.pipeline.PipelineScheduler --input <file.json> [--output <result.json>]");
                System.out.println("  java com.scoreme.pipeline.PipelineScheduler --n <tasks> --K <slots> [--density <d>] [--seed <s>]");
                return;
            }

            PSDSATURScheduler scheduler = new PSDSATURScheduler(instance);
            Models.ScheduleResult result = scheduler.solve();

            String jsonOutput = JsonUtils.serializeResult(result.toMap());

            if (argMap.containsKey("output")) {
                Files.writeString(Path.of(argMap.get("output")), jsonOutput);
                System.out.println("Results written to " + argMap.get("output"));
            } else {
                System.out.println(jsonOutput);
            }

        } catch (Exception e) {
            System.err.println("Error running pipeline scheduler: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--") && i + 1 < args.length) {
                map.put(args[i].substring(2), args[i + 1]);
                i++;
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Models.ProblemInstance parseInstanceFromJson(Map<String, Object> jsonMap) {
        List<String> taskNames = (List<String>) jsonMap.get("tasks");
        List<List<Object>> rawConflicts = (List<List<Object>>) jsonMap.get("conflicts");
        List<List<Object>> rawResources = (List<List<Object>>) jsonMap.get("resources");
        List<List<Object>> rawCapacities = (List<List<Object>>) jsonMap.get("capacities");
        List<List<Object>> rawWindows = (List<List<Object>>) jsonMap.get("windows");
        List<Object> rawWeights = (List<Object>) jsonMap.get("weights");
        int K = ((Number) jsonMap.get("K")).intValue();

        int n = taskNames.size();
        int d = 4;

        List<int[]> conflicts = new ArrayList<>();
        for (List<Object> pair : rawConflicts) {
            conflicts.add(new int[]{
                ((Number) pair.get(0)).intValue(),
                ((Number) pair.get(1)).intValue()
            });
        }

        List<double[]> slotCapacities = new ArrayList<>();
        for (List<Object> capRow : rawCapacities) {
            double[] cap = new double[d];
            for (int dim = 0; dim < d; dim++) {
                cap[dim] = ((Number) capRow.get(dim)).doubleValue();
            }
            slotCapacities.add(cap);
        }

        List<Models.Task> tasks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String id = taskNames.get(i);
            List<Object> resRow = rawResources.get(i);
            double[] res = new double[d];
            for (int dim = 0; dim < d; dim++) {
                res[dim] = ((Number) resRow.get(dim)).doubleValue();
            }
            List<Object> winRow = rawWindows.get(i);
            int winStart = ((Number) winRow.get(0)).intValue();
            int winEnd = ((Number) winRow.get(1)).intValue();
            double weight = ((Number) rawWeights.get(i)).doubleValue();

            tasks.add(new Models.Task(id, i, weight, winStart, winEnd, res));
        }

        Models.ConflictGraph graph = new Models.ConflictGraph(n, conflicts);
        return new Models.ProblemInstance(tasks, graph, slotCapacities, K, d);
    }
}
