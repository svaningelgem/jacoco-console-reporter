package io.github.svaningelgem;

import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Test utility to verify coverage data from real JaCoCo exec files
 */
public class CoverageVerifier {

    /**
     * Load and process all exec files in the given list of paths
     */
    private static CoverageResult loadAndProcessExecFiles(List<String> execFilePaths, String classesDir) throws IOException {
        // Standard JaCoCo way
        ExecutionDataStore standardStore = new ExecutionDataStore();
        SessionInfoStore sessionStore = new SessionInfoStore();

        for (String path : execFilePaths) {
            try (FileInputStream in = new FileInputStream(path)) {
                ExecutionDataReader reader = new ExecutionDataReader(in);
                reader.setExecutionDataVisitor(standardStore);
                reader.setSessionInfoVisitor(sessionStore);
                reader.read();
            }
        }

        // Our merged way
        ExecutionDataMerger merger = new ExecutionDataMerger();
        for (String path : execFilePaths) {
            try (FileInputStream in = new FileInputStream(path)) {
                ExecutionDataReader reader = new ExecutionDataReader(in);
                reader.setSessionInfoVisitor(sessionStore);
                // Use our visitor to read and merge data
                reader.setExecutionDataVisitor(data -> merger.mergeExecData(data));
                reader.read();
            }
        }

        // Now analyze coverage with both stores
        CoverageBuilder standardCoverage = analyzeCoverage(standardStore, classesDir);
        CoverageBuilder mergedCoverage = analyzeCoverage(merger.getMergedStore(), classesDir);

        int standardTotalLines = 0;
        int standardCoveredLines = 0;
        int mergedTotalLines = 0;
        int mergedCoveredLines = 0;

        // Calculate standard totals
        for (IClassCoverage cc : standardCoverage.getClasses()) {
            standardTotalLines += cc.getLineCounter().getTotalCount();
            standardCoveredLines += cc.getLineCounter().getCoveredCount();
        }

        // Calculate merged totals
        for (IClassCoverage cc : mergedCoverage.getClasses()) {
            mergedTotalLines += cc.getLineCounter().getTotalCount();
            mergedCoveredLines += cc.getLineCounter().getCoveredCount();
        }

        // Return results
        return new CoverageResult(
                standardTotalLines, standardCoveredLines,
                mergedTotalLines, mergedCoveredLines
        );
    }

    /**
     * Analyze coverage for a specific execution data store
     */
    private static CoverageBuilder analyzeCoverage(ExecutionDataStore executionData, String classesDir) throws IOException {
        CoverageBuilder coverageBuilder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(executionData, coverageBuilder);

        File classesDirectory = new File(classesDir);
        if (classesDirectory.exists()) {
            analyzer.analyzeAll(classesDirectory);
        }

        return coverageBuilder;
    }

    /**
     * Detailed line-by-line coverage analysis to find discrepancies
     */
    private static void analyzeLineByLineCoverage(
            List<String> execFilePaths,
            String classesDir,
            boolean showMissedLines) throws IOException {

        // Load with standard JaCoCo way
        ExecutionDataStore standardStore = new ExecutionDataStore();
        SessionInfoStore sessionStore = new SessionInfoStore();

        for (String path : execFilePaths) {
            try (FileInputStream in = new FileInputStream(path)) {
                ExecutionDataReader reader = new ExecutionDataReader(in);
                reader.setExecutionDataVisitor(standardStore);
                reader.setSessionInfoVisitor(sessionStore);
                reader.read();
            }
        }

        // Load with our merger
        ExecutionDataMerger merger = new ExecutionDataMerger();
        for (String path : execFilePaths) {
            try (FileInputStream in = new FileInputStream(path)) {
                ExecutionDataReader reader = new ExecutionDataReader(in);
                reader.setSessionInfoVisitor(sessionStore);
                reader.setExecutionDataVisitor(data -> merger.mergeExecData(data));
                reader.read();
            }
        }

        // Analyze coverage
        CoverageBuilder standardCoverage = analyzeCoverage(standardStore, classesDir);
        CoverageBuilder mergedCoverage = analyzeCoverage(merger.getMergedStore(), classesDir);

        // Maps for source file coverage
        Map<String, ISourceFileCoverage> standardSourceFiles = new HashMap<>();
        Map<String, ISourceFileCoverage> mergedSourceFiles = new HashMap<>();

        // Collect source file coverage
        for (ISourceFileCoverage sfc : standardCoverage.getSourceFiles()) {
            standardSourceFiles.put(sfc.getName(), sfc);
        }

        for (ISourceFileCoverage sfc : mergedCoverage.getSourceFiles()) {
            mergedSourceFiles.put(sfc.getName(), sfc);
        }

        // Find files in both collections
        List<String> commonFiles = new ArrayList<>(standardSourceFiles.keySet());
        commonFiles.retainAll(mergedSourceFiles.keySet());

        // Sort for consistent output
        Collections.sort(commonFiles);

        int filesWithDifferences = 0;
        int totalDifferentLines = 0;

        System.out.println("\n=== Line-by-Line Coverage Analysis ===");

        for (String file : commonFiles) {
            ISourceFileCoverage standardSfc = standardSourceFiles.get(file);
            ISourceFileCoverage mergedSfc = mergedSourceFiles.get(file);

            boolean hasDifference = false;
            List<Integer> differentLines = new ArrayList<>();

            // Only analyze if the line count matches
            if (standardSfc.getLastLine() == mergedSfc.getLastLine()) {
                for (int i = standardSfc.getFirstLine(); i <= standardSfc.getLastLine(); i++) {
                    ILine standardLine = standardSfc.getLine(i);
                    ILine mergedLine = mergedSfc.getLine(i);

                    // Check if lines have different status
                    if (standardLine.getStatus() != mergedLine.getStatus()) {
                        hasDifference = true;
                        differentLines.add(i);
                    }
                }
            }

            if (hasDifference) {
                filesWithDifferences++;
                totalDifferentLines += differentLines.size();

                System.out.println("\nFile: " + file);
                System.out.println("  Standard coverage: " + formatCoverage(standardSfc.getLineCounter()));
                System.out.println("  Merged coverage: " + formatCoverage(mergedSfc.getLineCounter()));
                System.out.println("  Different lines: " + differentLines.size());

                if (showMissedLines && differentLines.size() < 50) { // Limit output for readability
                    System.out.println("  Line numbers: " + differentLines);
                }
            }
        }

        System.out.println("\nSummary:");
        System.out.println("  Files with coverage differences: " + filesWithDifferences);
        System.out.println("  Total lines with differences: " + totalDifferentLines);
    }

    private static String formatCoverage(ICounter counter) {
        int total = counter.getTotalCount();
        int covered = counter.getCoveredCount();
        double percentage = total > 0 ? 100.0 * covered / total : 0;
        return String.format("%d/%d (%.2f%%)", covered, total, percentage);
    }

    /**
     * Main test method that can be run against real data
     */
    @Test
    public void testRealExecFiles() throws IOException {
        // Change these paths to match your environment
        String baseDir = "D:\\Temp\\output"; // e.g., "/path/to/project"
        String classesDir = baseDir + "/target/classes"; // Update if needed

        // Find all exec files
        List<String> execFiles = findExecFiles(baseDir);

        if (execFiles.isEmpty()) {
            System.out.println("No exec files found. Please check the base directory.");
            return;
        }

        System.out.println("Found " + execFiles.size() + " exec files:");
        execFiles.forEach(System.out::println);

        // Load and process all exec files
        CoverageResult result = loadAndProcessExecFiles(execFiles, classesDir);

        // Print results
        System.out.println("\n=== Coverage Results ===");
        System.out.println("Standard JaCoCo coverage: " +
                result.standardCoveredLines + "/" + result.standardTotalLines +
                String.format(" (%.2f%%)", result.standardCoveredPercentage()));

        System.out.println("Our merger coverage: " +
                result.mergedCoveredLines + "/" + result.mergedTotalLines +
                String.format(" (%.2f%%)", result.mergedCoveredPercentage()));

        System.out.println("Difference: " +
                String.format("%.2f%%", result.mergedCoveredPercentage() - result.standardCoveredPercentage()));

        // Find line by line differences to identify issues
        analyzeLineByLineCoverage(execFiles, classesDir, true);
    }

    /**
     * Find all JaCoCo exec files in the given base directory
     */
    private static List<String> findExecFiles(String baseDir) throws IOException {
        return Files.walk(Paths.get(baseDir))
                .filter(path -> path.toString().endsWith("jacoco.exec"))
                .map(Path::toString)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Result holder class for coverage data
     */
    private static class CoverageResult {
        final int standardTotalLines;
        final int standardCoveredLines;
        final int mergedTotalLines;
        final int mergedCoveredLines;

        CoverageResult(int standardTotalLines, int standardCoveredLines,
                       int mergedTotalLines, int mergedCoveredLines) {
            this.standardTotalLines = standardTotalLines;
            this.standardCoveredLines = standardCoveredLines;
            this.mergedTotalLines = mergedTotalLines;
            this.mergedCoveredLines = mergedCoveredLines;
        }

        double standardCoveredPercentage() {
            return standardTotalLines > 0 ? 100.0 * standardCoveredLines / standardTotalLines : 0;
        }

        double mergedCoveredPercentage() {
            return mergedTotalLines > 0 ? 100.0 * mergedCoveredLines / mergedTotalLines : 0;
        }
    }
}