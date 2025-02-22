package io.github.svaningelgem;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Generates a console-based coverage report from JaCoCo execution data.
 * This plugin provides a simple way to view coverage metrics directly in the console
 * without needing to generate HTML or XML reports.
 */
@Mojo(
        name = "report",
        defaultPhase = LifecyclePhase.VERIFY,
        threadSafe = true
)
public class JacocoConsoleReporterMojo extends AbstractMojo {
    // Define column widths
    static final int PACKAGE_WIDTH = 40;
    static final int METRICS_WIDTH = 25;

    // Define tree characters based on terminal capabilities
    private static final String LASTDIR_SPACE = "  ";
    private static final String VERTICAL_LINE = "| ";
    private static final String TEE = "+- ";
    private static final String CORNER = "`- ";

    static final String DIVIDER = getDivider();
    static final String HEADER_FORMAT = "%-" + PACKAGE_WIDTH + "s | %-" + METRICS_WIDTH + "s | %-" + METRICS_WIDTH + "s | %-" + METRICS_WIDTH + "s | %-" + METRICS_WIDTH + "s";
    static final String LINE_FORMAT = "%-" + PACKAGE_WIDTH + "s | %-" + METRICS_WIDTH + "s | %-" + METRICS_WIDTH + "s | %-" + METRICS_WIDTH + "s | %-" + METRICS_WIDTH + "s";

    /**
     * Location of the JaCoCo execution data file.
     */
    @Parameter(defaultValue = "${project.build.directory}/jacoco.exec", property = "jacocoExecFile", required = true)
    File jacocoExecFile;

    /**
     * Directory containing the compiled Java classes.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", property = "classesDirectory", required = true)
    File classesDirectory;

    public void execute() throws MojoExecutionException {
        if (!jacocoExecFile.exists()) {
            getLog().warn("No coverage data found at " + jacocoExecFile.getAbsolutePath() + "; ensure JaCoCo plugin ran with tests.");
            return;
        }

        try {
            ExecutionDataStore executionDataStore = loadExecutionData();
            IBundleCoverage bundle = analyzeCoverage(executionDataStore);
            DirectoryNode root = buildDirectoryTree(bundle);
            printCoverageReport(root);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to process JaCoCo data", e);
        }
    }

    /**
     * Loads JaCoCo execution data from the specified file.
     * Creates both execution data and session info stores to capture
     * all coverage information from the JaCoCo output file.
     *
     * @return Populated execution data store with coverage information
     * @throws IOException if there are issues reading the JaCoCo execution file
     */
    private @NotNull ExecutionDataStore loadExecutionData() throws IOException {
        ExecutionDataStore executionDataStore = new ExecutionDataStore();
        SessionInfoStore sessionInfoStore = new SessionInfoStore();

        try (FileInputStream in = new FileInputStream(jacocoExecFile)) {
            ExecutionDataReader reader = new ExecutionDataReader(in);
            reader.setExecutionDataVisitor(executionDataStore);
            reader.setSessionInfoVisitor(sessionInfoStore);
            reader.read();
        }

        return executionDataStore;
    }

    /**
     * Analyzes the compiled classes using the execution data to build coverage information.
     * Uses JaCoCo's analyzer to process all class files in the specified directory,
     * building a complete picture of code coverage.
     *
     * @param executionDataStore Contains the execution data from JaCoCo
     * @return A bundle containing all coverage information
     * @throws IOException if there are issues reading the class files
     */
    private IBundleCoverage analyzeCoverage(ExecutionDataStore executionDataStore) throws IOException {
        CoverageBuilder coverageBuilder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);
        analyzer.analyzeAll(classesDirectory);
        return coverageBuilder.getBundle("Project");
    }

    /**
     * Builds a tree structure representing the package hierarchy and their coverage metrics.
     *
     * @param bundle The bundle containing coverage data for all analyzed classes
     * @return The root node of the directory tree containing coverage information
     */
    private @NotNull DirectoryNode buildDirectoryTree(@NotNull IBundleCoverage bundle) {
        DirectoryNode root = new DirectoryNode("");
        for (IPackageCoverage packageCoverage : bundle.getPackages()) {
            String packageName = packageCoverage.getName().replace('.', '/');
            String[] pathComponents = packageName.split("/");
            DirectoryNode current = root;
            for (String component : pathComponents) {
                current = current.subdirectories.computeIfAbsent(component, DirectoryNode::new);
            }
            for (ISourceFileCoverage sourceFileCoverage : packageCoverage.getSourceFiles()) {
                String sourceFileName = sourceFileCoverage.getName();
                List<IClassCoverage> classesInFile = new ArrayList<>();
                for (IClassCoverage classCoverage : packageCoverage.getClasses()) {
                    if (classCoverage.getSourceFileName().equals(sourceFileName)) {
                        classesInFile.add(classCoverage);
                    }
                }
                CoverageMetrics metrics = new CoverageMetrics();
                metrics.totalClasses = classesInFile.size();
                metrics.coveredClasses = (int) classesInFile.stream()
                        .filter(c -> c.getMethodCounter().getCoveredCount() > 0)
                        .count();
                metrics.totalMethods = classesInFile.stream()
                        .mapToInt(c -> c.getMethodCounter().getTotalCount())
                        .sum();
                metrics.coveredMethods = classesInFile.stream()
                        .mapToInt(c -> c.getMethodCounter().getCoveredCount())
                        .sum();
                metrics.totalLines = sourceFileCoverage.getLineCounter().getTotalCount();
                metrics.coveredLines = sourceFileCoverage.getLineCounter().getCoveredCount();
                metrics.totalBranches = sourceFileCoverage.getBranchCounter().getTotalCount();
                metrics.coveredBranches = sourceFileCoverage.getBranchCounter().getCoveredCount();

                current.sourceFiles.add(new SourceFileCoverageData(sourceFileName, metrics));
            }
        }
        return root;
    }

    /**
     * Prints the coverage report to the console in a tree-like structure.
     * The report includes coverage metrics for each package and source file.
     *
     * @param root The root node of the directory tree containing coverage information
     */
    private void printCoverageReport(DirectoryNode root) {
        // Create format strings

        // Print header
        getLog().info("Overall Coverage Summary");
        getLog().info(String.format(HEADER_FORMAT, "Package", "Class, %", "Method, %", "Branch, %", "Line, %"));
        getLog().info(DIVIDER);

        // Print directory contents with tree structure
        printDirectoryTree(root, "", "", LINE_FORMAT);

        // Print total metrics
        getLog().info(DIVIDER);
        CoverageMetrics total = root.aggregateMetrics();
        getLog().info(String.format(LINE_FORMAT,
                "all classes",
                formatCoverage(total.coveredClasses, total.totalClasses),
                formatCoverage(total.coveredMethods, total.totalMethods),
                formatCoverage(total.coveredBranches, total.totalBranches),
                formatCoverage(total.coveredLines, total.totalLines)));
    }

    /**
     * Build a divider with certain widths
     */
    private static @NotNull String getDivider() {
        StringBuilder divider = new StringBuilder();
        for (int i = 0; i < PACKAGE_WIDTH; i++) divider.append("-");
        divider.append("-|-");
        for (int i = 0; i < METRICS_WIDTH; i++) divider.append("-");
        divider.append("-|-");
        for (int i = 0; i < METRICS_WIDTH; i++) divider.append("-");
        divider.append("-|-");
        for (int i = 0; i < METRICS_WIDTH; i++) divider.append("-");
        divider.append("-|-");
        for (int i = 0; i < METRICS_WIDTH; i++) divider.append("-");
        return divider.toString();
    }

    /**
     * Recursively prints the directory tree with coverage metrics.
     *
     * @param node        The current directory node
     * @param indent      The current indentation level
     * @param packageName The full package name up to this point
     * @param format      The format string for output formatting
     */
    private void printDirectoryTree(@NotNull DirectoryNode node, String indent, @NotNull String packageName, String format) {
        // First, determine if this node should be printed or collapsed
        boolean shouldPrintCurrentNode = !node.sourceFiles.isEmpty() || node.subdirectories.size() > 1;

        String currentPackage = packageName.isEmpty() ? node.name : packageName + "." + node.name;

        // If this is not root, and we should print this node, or if this is a leaf package node
        if (!currentPackage.isEmpty() && (shouldPrintCurrentNode || node.subdirectories.isEmpty())) {
            // Print package metrics
            CoverageMetrics aggregated = node.aggregateMetrics();
            getLog().info(String.format(format,
                    indent + currentPackage,
                    formatCoverage(aggregated.coveredClasses, aggregated.totalClasses),
                    formatCoverage(aggregated.coveredMethods, aggregated.totalMethods),
                    formatCoverage(aggregated.coveredBranches, aggregated.totalBranches),
                    formatCoverage(aggregated.coveredLines, aggregated.totalLines)));

            String childIndent = indent + "  ";

            // Print files
            for (int i = 0; i < node.sourceFiles.size(); i++) {
                SourceFileCoverageData file = node.sourceFiles.get(i);
                boolean isLastFile = i == node.sourceFiles.size() - 1 && node.subdirectories.isEmpty();
                String prefix = isLastFile ? CORNER : TEE;

                getLog().info(String.format(format,
                        childIndent + prefix + file.fileName,
                        formatCoverage(file.metrics.coveredClasses, file.metrics.totalClasses),
                        formatCoverage(file.metrics.coveredMethods, file.metrics.totalMethods),
                        formatCoverage(file.metrics.coveredBranches, file.metrics.totalBranches),
                        formatCoverage(file.metrics.coveredLines, file.metrics.totalLines)));
            }

            // For subdirectories, use the current indent
            indent = childIndent;
        }

        // Print subdirectories
        List<DirectoryNode> subdirs = new ArrayList<>(node.subdirectories.values());
        for (int i = 0; i < subdirs.size(); i++) {
            DirectoryNode subdir = subdirs.get(i);
            boolean isLastDir = i == subdirs.size() - 1;
            String nextIndent = indent + (isLastDir ? LASTDIR_SPACE : VERTICAL_LINE);

            // If this is the only subdirectory, and we have no files, pass through the current package name
            if (subdirs.size() == 1 && node.sourceFiles.isEmpty() && !shouldPrintCurrentNode) {
                printDirectoryTree(subdir, indent, currentPackage, format);
            } else {
                printDirectoryTree(subdir, nextIndent, "", format);
            }
        }
    }

    /**
     * Formats coverage metrics as a percentage with covered/total values.
     *
     * @param covered Number of covered items
     * @param total   Total number of items
     * @return Formatted string showing percentage and ratio (e.g., "75.00% (3/4)")
     */
    @Contract(pure = true)
    private @NotNull String formatCoverage(int covered, int total) {
        if (total == 0) return "100.00% (0/0)";
        double percentage = (double) covered / total * 100;
        return String.format("%6.2f%% (%d/%d)", percentage, covered, total);
    }

    /**
     * A node in the directory tree structure, representing a package or directory.
     * Each node contains coverage information and can have subdirectories and source files.
     */
    @Data
    @RequiredArgsConstructor
    static class DirectoryNode {
        /**
         * Name of the directory/package component
         */
        private final String name;
        /**
         * Map of subdirectories, keyed by name for easy tree construction
         */
        private final Map<String, DirectoryNode> subdirectories = new TreeMap<>();
        /**
         * List of source files in this directory, with their coverage metrics
         */
        private final List<SourceFileCoverageData> sourceFiles = new ArrayList<>();

        CoverageMetrics aggregateMetrics() {
            CoverageMetrics aggregated = new CoverageMetrics();
            sourceFiles.forEach(file -> aggregated.add(file.getMetrics()));
            subdirectories.values().forEach(subdir -> aggregated.add(subdir.aggregateMetrics()));
            return aggregated;
        }
    }

    /**
     * Container for coverage data related to a single source file.
     * Holds both the file's identity and its associated coverage metrics.
     */
    @Data
    @RequiredArgsConstructor
    static class SourceFileCoverageData {
        /**
         * Name of the source file being analyzed
         */
        private final String fileName;
        /**
         * Complete set of coverage metrics for this file
         */
        private final CoverageMetrics metrics;
    }

    /**
     * Comprehensive collection of coverage metrics for a source file or directory.
     * Tracks coverage at multiple levels: classes, methods, lines, and branches.
     * All metrics maintain both total count and covered count for percentage calculation.
     */
    @Data
    static class CoverageMetrics {
        /**
         * Total number of classes in the scope
         */
        private int totalClasses;
        /**
         * Number of classes that have any coverage
         */
        private int coveredClasses;
        /**
         * Total number of methods across all classes
         */
        private int totalMethods;
        /**
         * Number of methods that have been executed
         */
        private int coveredMethods;
        /**
         * Total number of lines of code
         */
        private int totalLines;
        /**
         * Number of lines that have been executed
         */
        private int coveredLines;
        /**
         * Total number of branches in conditional statements
         */
        private int totalBranches;
        /**
         * Number of branches that have been executed
         */
        private int coveredBranches;

        void add(@NotNull CoverageMetrics other) {
            totalClasses += other.totalClasses;
            coveredClasses += other.coveredClasses;
            totalMethods += other.totalMethods;
            coveredMethods += other.coveredMethods;
            totalLines += other.totalLines;
            coveredLines += other.coveredLines;
            totalBranches += other.totalBranches;
            coveredBranches += other.coveredBranches;
        }
    }
}