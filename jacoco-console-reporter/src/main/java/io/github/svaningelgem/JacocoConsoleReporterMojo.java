package io.github.svaningelgem;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionDataStore;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

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
    @Parameter(defaultValue = "true", property = "deferReporting")
    boolean deferReporting;

    /**
     * Option to show individual files in the report.
     * When false, only packages will be displayed.
     */
    @Parameter(defaultValue = "false", property = "showFiles")
    boolean showFiles;

    /**
     * Option to show individual files in the report.
     * When false, only packages will be displayed.
     */
    @Parameter(defaultValue = "true", property = "showTree")
    boolean showTree;

    /**
     * Option to show the summary in the report.
     * When false, only packages will be displayed.
     */
    @Parameter(defaultValue = "true", property = "showSummary")
    boolean showSummary;

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

    @Parameter(defaultValue = "0.1", property = "weightClassCoverage")
    double weightClassCoverage;

    @Parameter(defaultValue = "0.1", property = "weightMethodCoverage")
    double weightMethodCoverage;

    @Parameter(defaultValue = "0.4", property = "weightBranchCoverage")
    double weightBranchCoverage;

    @Parameter(defaultValue = "0.4", property = "weightLineCoverage")
    double weightLineCoverage;

    /**
     * When true, ignore the files in the build directory. 99.9% of the time these are automatically generated files.
     */
    @Parameter(defaultValue = "true", property = "ignoreFilesInBuildDirectory")
    boolean ignoreFilesInBuildDirectory;

    /**
     * Base directory for compiled output.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "targetDir")
    File targetDir;

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
     * JaCoCo plugin info for dependency discovery
     */
    private static final String JACOCO_GROUP_ID = "org.jacoco";
    private static final String JACOCO_ARTIFACT_ID = "jacoco-maven-plugin";
    private static final String DEFAULT_EXEC_FILENAME = "jacoco.exec";

    static final Set<File> collectedExecFilePaths = new HashSet<>();
    static final Set<File> collectedClassesPaths = new HashSet<>();

    final Set<Pattern> excludePatterns = new HashSet<>();

    public void execute() throws MojoExecutionException {
        additionalExecFiles.stream().map(File::getAbsoluteFile).forEach(collectedExecFilePaths::add);
        collectedExecFilePaths.add(jacocoExecFile.getAbsoluteFile());
        collectedClassesPaths.add(classesDirectory.getAbsoluteFile());
        getLog().debug("Collected Classes: " + collectedClassesPaths);
        if (jacocoExecFile.exists()) {
            getLog().debug("Added exec file from current module: " + jacocoExecFile.getAbsolutePath());
        }

        loadExclusionPatterns();

        if (scanModules) {
            scanForExecFiles();
        }

        // If we're deferring and this isn't the last module, return
        if (deferReporting && !shouldReport()) {
            getLog().info("Deferring JaCoCo reporting until the end of the build");
            return;
        }

        generateReport();
    }

    /**
     * Loads exclusion patterns from configuration and JaCoCo plugin settings
     */
    void loadExclusionPatterns() {
        addBuildDirExclusion();
        addJacocoExclusions();
        addSwaggerExclusions();
    }

    /**
     * Adds an exclusion pattern for files in the build directory if configured
     */
    void addBuildDirExclusion() {
        if (!ignoreFilesInBuildDirectory) {
            return;
        }

        try {
            // Convert the target directory path to a relative path pattern
            String buildDirPath = targetDir.getCanonicalPath();
            addExclusion(buildDirPath);
            getLog().debug("Added build directory exclusion pattern: " + buildDirPath);
        } catch (IOException e) {
            getLog().warn("Failed to add build directory exclusion: " + e.getMessage());
        }
    }

    /**
     * Extracts exclusion patterns from the JaCoCo plugin configuration
     */
    void addJacocoExclusions() {
        doSomethingForEachPluginConfiguration(JACOCO_GROUP_ID, JACOCO_ARTIFACT_ID, "excludes.exclude", excludePattern -> {
            addExclusion(excludePattern);
            getLog().debug("Excluded pattern: " + excludePattern);
        });
    }

    /**
     * Adds exclusion patterns for Swagger-generated files if present in the project
     */
    void addSwaggerExclusions() {
        // Check for Swagger code generation plugins and extract their configuration
        doSomethingForEachPluginConfiguration("io.swagger", "swagger-codegen-maven-plugin", Arrays.asList("output", "outputDirectory"), swaggerPattern -> {
            addExclusion(swaggerPattern);
            getLog().debug("Added Swagger exclusion pattern from outputDirectory: " + swaggerPattern);
        });

        // Check for SpringDoc OpenAPI generation
        doSomethingForEachPluginConfiguration("org.springdoc", "springdoc-openapi-maven-plugin", "outputDir", swaggerPattern -> {
            addExclusion(swaggerPattern);
            getLog().debug("Added SpringDoc OpenAPI exclusion pattern from outputDir: " + swaggerPattern);
        });

        // OpenAPI Generator plugin
        doSomethingForEachPluginConfiguration("org.openapitools", "openapi-generator-maven-plugin", Arrays.asList("outputDir", "output"), swaggerPattern -> {
            addExclusion(swaggerPattern);
            getLog().debug("Added OpenAPI Generator exclusion pattern from outputDir: " + swaggerPattern);
        });
    }

    /**
     * Converts a JaCoCo exclude pattern to a Java regex Pattern
     */
    void addExclusion(@NotNull String jacocoPattern) {
        jacocoPattern = jacocoPattern.replace("\\", "/");

        // Handle the .class extension if not present
        if (!jacocoPattern.endsWith(".class") && !jacocoPattern.endsWith("*")) {
            jacocoPattern += jacocoPattern.endsWith("/") ? "**/*.class" : "/**/*.class";
        }

        // Use temporary placeholders to avoid interference between replacements
        String regex = jacocoPattern
                .replace("**/", "__DOUBLE_STAR_SLASH__")
                .replace("**", "__DOUBLE_STAR__")
                .replace("*", "__STAR__")
                .replace(".", "__DOT__");

        // Now perform the actual replacements
        regex = regex
                .replace("__DOUBLE_STAR_SLASH__", "(?:[^/]*/)*")
                .replace("__DOUBLE_STAR__", ".*")
                .replace("__STAR__", "[^/]*")
                .replace("__DOT__", "\\.");

        getLog().debug("Converted pattern '" + jacocoPattern + "' to regex '" + regex + "'");

        Pattern pattern = Pattern.compile(regex);
        excludePatterns.add(pattern);
    }

    /**
     * Checks if a class should be excluded based on its name
     */
    boolean isExcluded(String className) {
        if (excludePatterns.isEmpty()) {
            return false;
        }

        // Convert class name to path format (replace dots with /)
        String classPath = className.replace('.', '/') + ".class";

        for (Pattern pattern : excludePatterns) {
            if (pattern.matcher(classPath).matches()) {
                getLog().debug("Excluding class: " + className);
                return true;
            }
        }

        return false;
    }

    void generateReport() throws MojoExecutionException {
        try {
            getLog().debug("Using exclusion patterns: " + excludePatterns);

            getLog().debug("Loading execution data");
            ExecutionDataStore executionDataStore = loadExecutionData();

            getLog().debug("Analyzing coverage");
            IBundleCoverage bundle = analyzeCoverage(executionDataStore);

            getLog().debug("Building internal tree model");
            DirectoryNode root = buildDirectoryTree(bundle);

            getLog().debug("Printing reports");
            printCoverageReport(root);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to process JaCoCo data", e);
        }
    }

    /**
     * Determines if this is the last module in a multi-module build
     * --> If so: start reporting
     */
    boolean shouldReport() {
        //Defer execution until the last project.
        return !deferReporting || project.getId().equals(mavenSession.getProjects().get(mavenSession.getProjects().size() - 1).getId());
    }

    /**
     * Scan for JaCoCo exec files in all modules
     */
    void scanForExecFiles() {
        getLog().info("Scanning for JaCoCo exec files");

        // Get the configured exec file pattern from JaCoCo plugin if available
        List<String> execPatterns = getConfiguredExecFilePatterns();

        // Start with the base directory
        scanDirectoryForExecFiles(baseDir, execPatterns);
    }

    void doSomethingForEachPluginConfiguration(String groupId, String artifactId, @NotNull Iterable<String> configValue, Consumer<String> configurationConsumer) {
        for (String config : configValue) {
            doSomethingForEachPluginConfiguration(groupId, artifactId, config, configurationConsumer);
        }
    }

    void doSomethingForEachPluginConfiguration(String groupId, String artifactId, @NotNull String configValue, Consumer<String> configurationConsumer) {
        final String[] parts = configValue.split("\\.");

        project.getBuildPlugins().stream()
                .filter(plugin -> groupId.equals(plugin.getGroupId())
                        && artifactId.equals(plugin.getArtifactId()))
                .forEach(plugin -> {
                    Xpp3Dom config = (Xpp3Dom) plugin.getConfiguration();
                    if (config == null) {
                        return;
                    }

                    // Queue of nodes to process at the current level
                    Queue<Xpp3Dom> currentLevelNodes = new LinkedList<>();
                    currentLevelNodes.add(config);

                    // Process each part of the path
                    for (int i = 0; i < parts.length; i++) {
                        String part = parts[i];
                        Queue<Xpp3Dom> nextLevelNodes = new LinkedList<>();

                        // Process all nodes at the current level
                        for (Xpp3Dom currentNode : currentLevelNodes) {
                            // Get all children with matching name
                            Xpp3Dom[] children = currentNode.getChildren(part);
                            if (children != null) {
                                // Add all matching children to the next level queue
                                Collections.addAll(nextLevelNodes, children);
                            }
                        }

                        // If this is the last part in the path, apply consumer to all matching nodes
                        if (i == parts.length - 1) {
                            nextLevelNodes.forEach(node -> {
                                if (node == null) return;
                                String value = node.getValue();
                                if (value == null) return;
                                value = value.trim();
                                if (value.isEmpty()) return;

                                configurationConsumer.accept(value);
                            });
                            return; // We're done processing
                        }

                        // If no matching nodes found at this level, stop processing
                        if (nextLevelNodes.isEmpty()) {
                            return;
                        }

                        // Continue with the next level
                        currentLevelNodes = nextLevelNodes;
                    }
                });
    }

    /**
     * Get the configured JaCoCo exec file patterns from the project
     */
    @NotNull List<String> getConfiguredExecFilePatterns() {
        List<String> patterns = new ArrayList<>();
        // Add the default pattern
        patterns.add(DEFAULT_EXEC_FILENAME);

        doSomethingForEachPluginConfiguration(JACOCO_GROUP_ID, JACOCO_ARTIFACT_ID, "destFile", destFile -> {
            // Extract just the filename
            patterns.add(new File(destFile).getName());
            getLog().debug("Found JaCoCo destFile: " + destFile);
        });

        return patterns;
    }

    /**
     * Recursively scan directories for JaCoCo exec files
     */
    void scanDirectoryForExecFiles(@NotNull File dir, List<String> execPatterns) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        // Check for target directory with an exec file
        File targetDir = new File(dir, "target");
        if (targetDir.exists() && targetDir.isDirectory()) {
            File[] execFiles = targetDir.listFiles((d, name) ->
                    execPatterns.stream().anyMatch(name::equals));

            if (execFiles != null) {
                for (File execFile : execFiles) {
                    collectedExecFilePaths.add(execFile);
                    getLog().debug("Found exec file: " + execFile.getAbsolutePath());
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
     * Loads JaCoCo execution data from the specified files with proper deduplication.
     * Uses the ExecutionDataMerger to ensure line and branch coverage isn't duplicated
     * when aggregating coverage from multiple modules that share common code.
     *
     * @return Populated execution data store with deduplicated coverage information
     * @throws IOException if there are issues reading the JaCoCo execution files
     */
    @NotNull ExecutionDataStore loadExecutionData() throws IOException {
        getLog().debug("Loading execution data with line-level deduplication");
        ExecutionDataMerger merger = new ExecutionDataMerger();

        // Pass all exec files to the merger
        ExecutionDataStore executionDataStore = merger.loadExecutionData(collectedExecFilePaths);

        int fileCount = (int) collectedExecFilePaths.stream()
                .filter(file -> file != null && file.exists())
                .count();

        getLog().debug(String.format("Processed %d exec files containing data for %d unique classes",
                fileCount, merger.getUniqueClassCount()));

        return executionDataStore;
    }

    /**
     * Analyzes the compiled classes using the execution data to build coverage information.
     * Uses JaCoCo's analyzer to process all class files in the specified directory,
     * building a complete picture of code coverage. Applies exclusion patterns to filter
     * out classes that should not be included in coverage analysis.
     *
     * @param executionDataStore Contains the execution data from JaCoCo
     * @return A bundle containing all coverage information
     * @throws IOException if there are issues reading the class files
     */
    @NotNull IBundleCoverage analyzeCoverage(@NotNull ExecutionDataStore executionDataStore) throws IOException {
        // Create custom CoverageBuilder that filters excluded classes
        CoverageBuilder coverageBuilder = new CoverageBuilder() {
            @Override
            public void visitCoverage(IClassCoverage coverage) {
                String className = coverage.getName().replace('/', '.');
                if (!isExcluded(className)) {
                    super.visitCoverage(coverage);
                } else {
                    getLog().debug("Excluded from coverage: " + className);
                }
            }
        };

        Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);

        for (File classPath : collectedClassesPaths) {
            if (classPath == null || !classPath.exists()) {
                continue;
            }
            getLog().debug("Analyzing class files in: " + classPath.getAbsolutePath());
            Files.walkFileTree(classPath.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                    String filePath = file.toString().toLowerCase(Locale.ENGLISH);
                    if (filePath.endsWith(".class")) {
                        try (FileInputStream in = new FileInputStream(file.toFile())) {
                            analyzer.analyzeClass(in, file.toString());
                        } catch (Exception e) {
                            getLog().debug("Error analyzing class file: " + file + ": " + e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return coverageBuilder.getBundle("Project");
    }

    /**
     * Prints the coverage report to the console in a tree-like structure.
     * The report includes coverage metrics for each package and source file.
     *
     * @param root The root node of the directory tree containing coverage information
     */
    void printCoverageReport(@NotNull DirectoryNode root) {
        printTree(root);
        printSummary(root);
    }

    void printSummary(@NotNull DirectoryNode root) {
        if (!showSummary) return;

        CoverageMetrics total = root.getMetrics();

        getLog().info("Overall Coverage Summary");
        getLog().info("------------------------");
        getLog().info("Class coverage : " + Defaults.getInstance().formatCoverage(total.getCoveredClasses(), total.getTotalClasses()));
        getLog().info("Method coverage: " + Defaults.getInstance().formatCoverage(total.getCoveredMethods(), total.getTotalMethods()));
        getLog().info("Branch coverage: " + Defaults.getInstance().formatCoverage(total.getCoveredBranches(), total.getTotalBranches()));
        getLog().info("Line coverage  : " + Defaults.getInstance().formatCoverage(total.getCoveredLines(), total.getTotalLines()));

        double combinedCoverage = 0;
        double combinedTotalCoverage = 0;
        combinedCoverage += total.getCoveredClasses() * weightClassCoverage;
        combinedTotalCoverage += total.getTotalClasses() * weightClassCoverage;
        combinedCoverage += total.getCoveredMethods() * weightMethodCoverage;
        combinedTotalCoverage += total.getTotalMethods() * weightMethodCoverage;
        combinedCoverage += total.getCoveredBranches() * weightBranchCoverage;
        combinedTotalCoverage += total.getTotalBranches() * weightBranchCoverage;
        combinedCoverage += total.getCoveredLines() * weightLineCoverage;
        combinedTotalCoverage += total.getTotalLines() * weightLineCoverage;

        getLog().info(
                String.format("Combined coverage: %5.2f%% (Class %d%%, Method %d%%, Branch %d%%, Line %d%%)",
                        combinedTotalCoverage == 0 ? 100. : combinedCoverage * 100.0 / combinedTotalCoverage,
                        (int) (weightClassCoverage * 100.0),
                        (int) (weightMethodCoverage * 100.0),
                        (int) (weightBranchCoverage * 100.0),
                        (int) (weightLineCoverage * 100.0))
        );

    }

    void printTree(@NotNull DirectoryNode root) {
        if (!showTree) return;

        // Print header
        getLog().info("Overall Coverage Summary");
        getLog().info(String.format(Defaults.getInstance().lineFormat, "Package", "Class, %", "Method, %", "Branch, %", "Line, %"));
        getLog().info(Defaults.getInstance().divider);

        // Print the tree structure - start with an empty prefix for root
        root.printTree(getLog(), "", Defaults.getInstance().lineFormat, "", showFiles);

        // Print total metrics
        getLog().info(Defaults.getInstance().divider);
        CoverageMetrics total = root.getMetrics();
        getLog().info(String.format(Defaults.getInstance().lineFormat,
                "all classes",
                Defaults.getInstance().formatCoverage(total.getCoveredClasses(), total.getTotalClasses()),
                Defaults.getInstance().formatCoverage(total.getCoveredMethods(), total.getTotalMethods()),
                Defaults.getInstance().formatCoverage(total.getCoveredBranches(), total.getTotalBranches()),
                Defaults.getInstance().formatCoverage(total.getCoveredLines(), total.getTotalLines())));
    }

    /**
     * Builds a tree structure representing the package hierarchy and their coverage metrics.
     * Modified to use SourceFileNode instead of SourceFileCoverageData.
     *
     * @param bundle The bundle containing coverage data for all analyzed classes
     * @return The root node of the directory tree containing coverage information
     */
    @NotNull DirectoryNode buildDirectoryTree(@NotNull IBundleCoverage bundle) {
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