package io.github.svaningelgem;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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
 * Maven plugin for generating a console-based coverage report from JaCoCo execution data.
 * This plugin reads JaCoCo execution data and class files to create a hierarchical
 * coverage report directly in the console, making it easy to view coverage metrics
 * without needing to generate HTML or XML reports.
 *
 * @goal report
 * @phase verify
 */
@Mojo(name = "report")
public class JacocoConsoleReporterMojo extends AbstractMojo {

    /**
     * Location of the JaCoCo execution data file (jacoco.exec).
     */
    @Parameter(defaultValue = "${project.build.directory}/jacoco.exec", property = "jacocoExecFile", required = true)
    File jacocoExecFile;

    /**
     * Directory containing the compiled Java classes.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", property = "classesDirectory", required = true)
    File classesDirectory;

    /**
     * Executes the plugin, generating a console-based coverage report.
     * The report includes coverage metrics for classes, methods, branches, and lines,
     * organized in a tree structure by package.
     *
     * @throws MojoExecutionException if there's an error processing the JaCoCo data or class files
     */
    public void execute() throws MojoExecutionException {
        if (!jacocoExecFile.exists()) {
            getLog().warn("No coverage data found at " + jacocoExecFile.getAbsolutePath() + "; ensure JaCoCo plugin ran with tests.");
            return;
        }

        try {
            // Load execution data
            ExecutionDataStore executionDataStore = new ExecutionDataStore();
            SessionInfoStore sessionInfoStore = new SessionInfoStore();
            try (FileInputStream in = new FileInputStream(jacocoExecFile)) {
                ExecutionDataReader reader = new ExecutionDataReader(in);
                reader.setExecutionDataVisitor(executionDataStore);
                reader.setSessionInfoVisitor(sessionInfoStore);
                reader.read();
            }

            // Analyze class files
            CoverageBuilder coverageBuilder = new CoverageBuilder();
            Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);
            analyzer.analyzeAll(classesDirectory);

            IBundleCoverage bundle = coverageBuilder.getBundle("Project");

            // Build directory tree and print report
            DirectoryNode root = buildDirectoryTree(bundle);
            printCoverageReport(root);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to process JaCoCo data", e);
        }
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
        // Define column widths
        int packageWidth = 40;
        int metricsWidth = 25;

        // Create format strings
        String headerFormat = "%-" + packageWidth + "s | %-" + metricsWidth + "s | %-" + metricsWidth + "s | %-" + metricsWidth + "s | %-" + metricsWidth + "s";
        String lineFormat = "%-" + packageWidth + "s | %-" + metricsWidth + "s | %-" + metricsWidth + "s | %-" + metricsWidth + "s | %-" + metricsWidth + "s";

        // Print header
        getLog().info("Overall Coverage Summary");
        getLog().info(String.format(headerFormat, "Package", "Class, %", "Method, %", "Branch, %", "Line, %"));

        // Print divider line
        StringBuilder divider = new StringBuilder();
        for (int i = 0; i < packageWidth; i++) divider.append("-");
        divider.append("-|-");
        for (int i = 0; i < metricsWidth; i++) divider.append("-");
        divider.append("-|-");
        for (int i = 0; i < metricsWidth; i++) divider.append("-");
        divider.append("-|-");
        for (int i = 0; i < metricsWidth; i++) divider.append("-");
        divider.append("-|-");
        for (int i = 0; i < metricsWidth; i++) divider.append("-");
        getLog().info(divider.toString());

        // Print directory contents with tree structure
        printDirectoryTree(root, "", "", lineFormat);

        // Print total metrics
        getLog().info(divider.toString());
        CoverageMetrics total = root.aggregateMetrics();
        getLog().info(String.format(lineFormat,
                "all classes",
                formatCoverage(total.coveredClasses, total.totalClasses),
                formatCoverage(total.coveredMethods, total.totalMethods),
                formatCoverage(total.coveredBranches, total.totalBranches),
                formatCoverage(total.coveredLines, total.totalLines)));
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

        // If this is not root and we should print this node, or if this is a leaf package node
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
                String prefix = isLastFile ? "└─ " : "├─ ";

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
            String nextIndent = indent + (isLastDir ? "  " : "│ ");

            // If this is the only subdirectory and we have no files, pass through the current package name
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
     * Represents a node in the directory tree structure, containing coverage information
     * for a package and its source files.
     */
    static class DirectoryNode {
        /**
         * Name of the directory/package component
         */
        String name;
        /**
         * Map of subdirectories, keyed by name
         */
        Map<String, DirectoryNode> subdirectories = new TreeMap<>();
        /**
         * List of source files in this directory
         */
        List<SourceFileCoverageData> sourceFiles = new ArrayList<>();

        /**
         * Creates a new directory node with the given name.
         *
         * @param name The name of the directory/package component
         */
        DirectoryNode(String name) {
            this.name = name;
        }

        /**
         * Aggregates coverage metrics for this directory and all its subdirectories.
         *
         * @return Combined coverage metrics for the entire subtree
         */
        CoverageMetrics aggregateMetrics() {
            CoverageMetrics aggregated = new CoverageMetrics();
            for (SourceFileCoverageData file : sourceFiles) {
                aggregated.add(file.metrics);
            }
            for (DirectoryNode subdir : subdirectories.values()) {
                aggregated.add(subdir.aggregateMetrics());
            }
            return aggregated;
        }
    }

    /**
     * Contains coverage data for a single source file.
     */
    static class SourceFileCoverageData {
        /**
         * Name of the source file
         */
        String fileName;
        /**
         * Coverage metrics for the file
         */
        CoverageMetrics metrics;

        /**
         * Creates coverage data for a source file.
         *
         * @param fileName Name of the source file
         * @param metrics  Coverage metrics for the file
         */
        SourceFileCoverageData(String fileName, CoverageMetrics metrics) {
            this.fileName = fileName;
            this.metrics = metrics;
        }
    }

    /**
     * Holds various coverage metrics for a source file or directory.
     */
    static class CoverageMetrics {
        /**
         * Total number of classes and number of classes with coverage
         */
        int totalClasses, coveredClasses;
        /**
         * Total number of methods and number of methods with coverage
         */
        int totalMethods, coveredMethods;
        /**
         * Total number of lines and number of lines with coverage
         */
        int totalLines, coveredLines;
        /**
         * Total number of branches and number of branches with coverage
         */
        int totalBranches, coveredBranches;

        /**
         * Adds another set of metrics to this one.
         * Used for aggregating metrics across multiple files or directories.
         *
         * @param other The metrics to add to this one
         */
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
