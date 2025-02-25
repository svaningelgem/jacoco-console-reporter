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
    static final int PACKAGE_WIDTH = 100;
    static final int METRICS_WIDTH = 20;

    // Define tree characters based on terminal capabilities
    private static final String LASTDIR_SPACE = " ";
    private static final String VERTICAL_LINE = "│";
    private static final String TEE = "├─";
    private static final String CORNER = "└─";

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

    /**
     * Option to defer reporting until the end (for multi-module projects).
     * When true, the plugin will not report during module execution.
     */
    @Parameter(defaultValue = "false", property = "deferReporting", required = false)
    boolean deferReporting;

    /**
     * Option to show individual files in the report.
     * When false, only packages will be displayed.
     */
    @Parameter(defaultValue = "true", property = "showFiles", required = false)
    boolean showFiles;

    /**
     * Additional exec files to include in the report.
     * Useful for aggregating multiple module reports.
     */
    @Parameter(property = "additionalExecFiles")
    List<File> additionalExecFiles = new ArrayList<>();

    /**
     * Option to scan for exec files in project modules.
     * When true, will automatically discover all jacoco.exec files in the project.
     */
    @Parameter(defaultValue = "false", property = "scanModules", required = false)
    boolean scanModules;

    /**
     * Base directory for module scanning.
     */
    @Parameter(defaultValue = "${project.basedir}", property = "baseDir", required = false)
    File baseDir;

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    org.apache.maven.project.MavenProject project;

    /**
     * The Maven session.
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    org.apache.maven.execution.MavenSession mavenSession;

    /**
     * Track which exec files have been processed
     */
    private final List<String> processedExecFiles = new ArrayList<>();

    /**
     * JaCoCo plugin info for dependency discovery
     */
    private static final String JACOCO_GROUP_ID = "org.jacoco";
    private static final String JACOCO_ARTIFACT_ID = "jacoco-maven-plugin";
    private static final String DEFAULT_EXEC_FILENAME = "jacoco.exec";

    public void execute() throws MojoExecutionException {
        // Check if JaCoCo plugin is in the project
        boolean hasJacocoPlugin = checkForJacocoPlugin();

        // Even when deferring, collect the current module's exec file
        if (jacocoExecFile.exists() && !additionalExecFiles.contains(jacocoExecFile)) {
            additionalExecFiles.add(jacocoExecFile);
            getLog().debug("Added exec file from current module: " + jacocoExecFile.getAbsolutePath());
        }

        // Scan for exec files if requested
        if (scanModules) {
            scanForExecFiles();
        }

        // If we're deferring and this isn't the last module, return
        if (deferReporting && !shouldReport()) {
            getLog().info("Deferring JaCoCo reporting until the end of the build");
            return;
        }

        if (additionalExecFiles.isEmpty()) {
            if (hasJacocoPlugin) {
                getLog().warn("No coverage data found; ensure JaCoCo plugin ran with tests.");
            } else {
                getLog().warn("No JaCoCo plugin found in project; no coverage data will be available.");
            }
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
     * Check if the JaCoCo plugin is configured in the current project
     */
    private boolean checkForJacocoPlugin() {
        return project.getBuildPlugins().stream()
                .anyMatch(plugin ->
                        JACOCO_GROUP_ID.equals(plugin.getGroupId()) &&
                                JACOCO_ARTIFACT_ID.equals(plugin.getArtifactId()));
    }

    /**
     * Determines if this is the last module in a multi-module build
     * --> If so: start reporting
     */
    private boolean shouldReport() {
        //Defer execution until the last project.
        return !deferReporting || project.getId().equals(mavenSession.getProjects().get(mavenSession.getProjects().size() - 1).getId());
    }

    /**
     * Scan for JaCoCo exec files in all modules
     */
    private void scanForExecFiles() {
        getLog().info("Scanning for JaCoCo exec files");

        // Get the configured exec file pattern from JaCoCo plugin if available
        List<String> execPatterns = getConfiguredExecFilePatterns();

        // Start with the base directory
        scanDirectoryForExecFiles(baseDir, execPatterns);
    }

    /**
     * Get the configured JaCoCo exec file patterns from the project
     */
    private List<String> getConfiguredExecFilePatterns() {
        List<String> patterns = new ArrayList<>();
        // Add the default pattern
        patterns.add(DEFAULT_EXEC_FILENAME);

        project.getBuildPlugins().stream()
                .filter(plugin -> JACOCO_GROUP_ID.equals(plugin.getGroupId())
                        && JACOCO_ARTIFACT_ID.equals(plugin.getArtifactId()))
                .forEach(plugin -> {
                    Object config = plugin.getConfiguration();
                    if (config != null) {
                        try {
                            // This is a very simplified approach - in a real implementation
                            // you would need more robust XML parsing of the configuration
                            String configStr = config.toString();
                            if (configStr.contains("<destFile>")) {
                                int start = configStr.indexOf("<destFile>") + 10;
                                int end = configStr.indexOf("</destFile>", start);
                                if (end > start) {
                                    String destFile = configStr.substring(start, end).trim();
                                    // Extract just the filename
                                    patterns.add(new File(destFile).getName());
                                }
                            }
                        } catch (Exception e) {
                            getLog().debug("Error parsing JaCoCo configuration: " + e.getMessage());
                        }
                    }
                });

        return patterns;
    }

    /**
     * Recursively scan directories for JaCoCo exec files
     */
    private void scanDirectoryForExecFiles(File dir, List<String> execPatterns) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        // Check for target directory with exec file
        File targetDir = new File(dir, "target");
        if (targetDir.exists() && targetDir.isDirectory()) {
            File[] execFiles = targetDir.listFiles((d, name) ->
                    execPatterns.stream().anyMatch(pattern -> name.equals(pattern)));

            if (execFiles != null) {
                for (File execFile : execFiles) {
                    if (!additionalExecFiles.contains(execFile)) {
                        additionalExecFiles.add(execFile);
                        getLog().debug("Found exec file: " + execFile.getAbsolutePath());
                    }
                }
            }
        }

        // Recursively check subdirectories, but skip some common directories to avoid deep scanning
        File[] subdirs = dir.listFiles(file ->
                file.isDirectory() &&
                        !file.getName().equals("target") &&
                        !file.getName().equals("node_modules") &&
                        !file.getName().startsWith("."));

        if (subdirs != null) {
            for (File subdir : subdirs) {
                scanDirectoryForExecFiles(subdir, execPatterns);
            }
        }
    }

    /**
     * Loads JaCoCo execution data from the specified files.
     * Creates both execution data and session info stores to capture
     * all coverage information from the JaCoCo output files.
     *
     * @return Populated execution data store with coverage information
     * @throws IOException if there are issues reading the JaCoCo execution files
     */
    private @NotNull ExecutionDataStore loadExecutionData() throws IOException {
        ExecutionDataStore executionDataStore = new ExecutionDataStore();
        SessionInfoStore sessionInfoStore = new SessionInfoStore();

        // Load all exec files
        for (File execFile : additionalExecFiles) {
            if (execFile.exists()) {
                String execPath = execFile.getAbsolutePath();
                if (!processedExecFiles.contains(execPath)) {
                    loadExecFile(execFile, executionDataStore, sessionInfoStore);
                    processedExecFiles.add(execPath);
                    getLog().debug("Processed exec file: " + execPath);
                } else {
                    getLog().debug("Skipping already processed exec file: " + execPath);
                }
            } else {
                getLog().warn("Exec file not found: " + execFile.getAbsolutePath());
            }
        }

        return executionDataStore;
    }

    /**
     * Loads an individual JaCoCo execution data file
     */
    private void loadExecFile(File execFile, ExecutionDataStore executionDataStore, SessionInfoStore sessionInfoStore) throws IOException {
        try (FileInputStream in = new FileInputStream(execFile)) {
            ExecutionDataReader reader = new ExecutionDataReader(in);
            reader.setExecutionDataVisitor(executionDataStore);
            reader.setSessionInfoVisitor(sessionInfoStore);
            reader.read();
        }
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
     * Truncates a string in the middle if it exceeds maxLength
     * Example: "com.example.very.long.package.name" -> "com.example...kage.name"
     */
    private String truncateMiddle(String input, int maxLength) {
        if (input.length() <= maxLength) {
            return input;
        }

        int prefixLength = (maxLength - 3) / 2;
        int suffixLength = maxLength - 3 - prefixLength;

        return input.substring(0, prefixLength) + "..." +
                input.substring(input.length() - suffixLength);
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
                    truncateMiddle(indent + currentPackage, PACKAGE_WIDTH),
                    formatCoverage(aggregated.coveredClasses, aggregated.totalClasses),
                    formatCoverage(aggregated.coveredMethods, aggregated.totalMethods),
                    formatCoverage(aggregated.coveredBranches, aggregated.totalBranches),
                    formatCoverage(aggregated.coveredLines, aggregated.totalLines)));

            String childIndent = indent + " ";

            // Print files if showFiles is enabled
            if (showFiles) {
                for (int i = 0; i < node.sourceFiles.size(); i++) {
                    SourceFileCoverageData file = node.sourceFiles.get(i);
                    boolean isLastFile = i == node.sourceFiles.size() - 1 && node.subdirectories.isEmpty();
                    String prefix = isLastFile ? CORNER : TEE;

                    getLog().info(String.format(format,
                            truncateMiddle(childIndent + prefix + file.fileName, PACKAGE_WIDTH),
                            formatCoverage(file.metrics.coveredClasses, file.metrics.totalClasses),
                            formatCoverage(file.metrics.coveredMethods, file.metrics.totalMethods),
                            formatCoverage(file.metrics.coveredBranches, file.metrics.totalBranches),
                            formatCoverage(file.metrics.coveredLines, file.metrics.totalLines)));
                }
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
        return String.format("%5.2f%% (%d/%d)", percentage, covered, total);
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