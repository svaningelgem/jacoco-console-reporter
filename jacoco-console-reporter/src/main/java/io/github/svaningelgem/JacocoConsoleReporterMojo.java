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
import java.util.*;

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
    @Parameter(defaultValue = "false", property = "deferReporting")
    boolean deferReporting;

    /**
     * Option to show individual files in the report.
     * When false, only packages will be displayed.
     */
    @Parameter(defaultValue = "true", property = "showFiles")
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
    @Parameter(defaultValue = "false", property = "scanModules")
    boolean scanModules;

    /**
     * Base directory for module scanning.
     */
    @Parameter(defaultValue = "${project.basedir}", property = "baseDir")
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
    private @NotNull List<String> getConfiguredExecFilePatterns() {
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
    private void scanDirectoryForExecFiles(@NotNull File dir, List<String> execPatterns) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        // Check for target directory with exec file
        File targetDir = new File(dir, "target");
        if (targetDir.exists() && targetDir.isDirectory()) {
            File[] execFiles = targetDir.listFiles((d, name) ->
                    execPatterns.stream().anyMatch(name::equals));

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
     * Prints the coverage report to the console in a tree-like structure.
     * The report includes coverage metrics for each package and source file.
     *
     * @param root The root node of the directory tree containing coverage information
     */
    private void printCoverageReport(DirectoryNode root) {
        // Print header
        getLog().info("Overall Coverage Summary");
        getLog().info(String.format(Defaults.HEADER_FORMAT, "Package", "Class, %", "Method, %", "Branch, %", "Line, %"));
        getLog().info(Defaults.DIVIDER);

        // Print the tree structure - start with an empty prefix for root
        root.printTree(getLog(), "", Defaults.LINE_FORMAT, "", showFiles);

        // Print total metrics
        getLog().info(Defaults.DIVIDER);
        CoverageMetrics total = root.aggregateMetrics();
        getLog().info(String.format(Defaults.LINE_FORMAT,
                "all classes",
                Defaults.formatCoverage(total.getCoveredClasses(), total.getTotalClasses()),
                Defaults.formatCoverage(total.getCoveredMethods(), total.getTotalMethods()),
                Defaults.formatCoverage(total.getCoveredBranches(), total.getTotalBranches()),
                Defaults.formatCoverage(total.getCoveredLines(), total.getTotalLines())));
    }

    /**
     * Builds a tree structure representing the package hierarchy and their coverage metrics.
     * Modified to use SourceFileNode instead of SourceFileCoverageData.
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
                current = current.getSubdirectories().computeIfAbsent(component, DirectoryNode::new);
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
                metrics.setTotalClasses(classesInFile.size());
                metrics.setCoveredClasses((int) classesInFile.stream()
                        .filter(c -> c.getMethodCounter().getCoveredCount() > 0)
                        .count());
                metrics.setTotalMethods(classesInFile.stream()
                        .mapToInt(c -> c.getMethodCounter().getTotalCount())
                        .sum());
                metrics.setCoveredMethods(classesInFile.stream()
                        .mapToInt(c -> c.getMethodCounter().getCoveredCount())
                        .sum());
                metrics.setTotalLines(sourceFileCoverage.getLineCounter().getTotalCount());
                metrics.setCoveredLines(sourceFileCoverage.getLineCounter().getCoveredCount());
                metrics.setTotalBranches(sourceFileCoverage.getBranchCounter().getTotalCount());
                metrics.setCoveredBranches(sourceFileCoverage.getBranchCounter().getCoveredCount());

                current.getSourceFiles().add(new SourceFileNode(sourceFileName, metrics));
            }
        }
        return root;
    }
}