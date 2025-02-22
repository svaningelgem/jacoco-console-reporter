package com.github;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;

import java.io.*;
import java.util.*;

@Mojo(name = "report")
public class JacocoConsoleReporterMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.directory}/jacoco.exec", property = "jacocoExecFile", required = true)
    private File jacocoExecFile;

    @Parameter(defaultValue = "${project.build.outputDirectory}", property = "classesDirectory", required = true)
    private File classesDirectory;

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

    private DirectoryNode buildDirectoryTree(IBundleCoverage bundle) {
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

                current.localMetrics.add(metrics);
                SourceFileCoverageData sourceFileData = new SourceFileCoverageData(sourceFileName, metrics);
                current.sourceFiles.add(sourceFileData);
            }
        }
        return root;
    }

    private void printCoverageReport(DirectoryNode root) {
        getLog().info("Overall Coverage Summary");
        getLog().info("Package | Class, % | Method, % | Branch, % | Line, %");
        printDirectory(root, "");
        getLog().info("all classes | " + formatCoverage(root.aggregateMetrics().coveredClasses, root.aggregateMetrics().totalClasses) + " | " +
                formatCoverage(root.aggregateMetrics().coveredMethods, root.aggregateMetrics().totalMethods) + " | " +
                formatCoverage(root.aggregateMetrics().coveredBranches, root.aggregateMetrics().totalBranches) + " | " +
                formatCoverage(root.aggregateMetrics().coveredLines, root.aggregateMetrics().totalLines));
    }

    private void printDirectory(DirectoryNode node, String packageName) {
        String currentPackage = packageName.isEmpty() ? node.name : packageName + "." + node.name;
        CoverageMetrics aggregated = node.aggregateMetrics();
        getLog().info(currentPackage + " | " + formatCoverage(aggregated.coveredClasses, aggregated.totalClasses) + " | " +
                formatCoverage(aggregated.coveredMethods, aggregated.totalMethods) + " | " +
                formatCoverage(aggregated.coveredBranches, aggregated.totalBranches) + " | " +
                formatCoverage(aggregated.coveredLines, aggregated.totalLines));
        for (SourceFileCoverageData file : node.sourceFiles) {
            getLog().info("File: " + file.fileName + " | " + formatCoverage(file.metrics.coveredClasses, file.metrics.totalClasses) + " | " +
                    formatCoverage(file.metrics.coveredMethods, file.metrics.totalMethods) + " | " +
                    formatCoverage(file.metrics.coveredBranches, file.metrics.totalBranches) + " | " +
                    formatCoverage(file.metrics.coveredLines, file.metrics.totalLines));
        }
        for (DirectoryNode subdir : node.subdirectories.values()) {
            printDirectory(subdir, currentPackage);
        }
    }

    private String formatCoverage(int covered, int total) {
        if (total == 0) return "100.00% (0/0)";
        double percentage = (double) covered / total * 100;
        return String.format("%.2f%% (%d/%d)", percentage, covered, total);
    }

    static class DirectoryNode {
        String name;
        Map<String, DirectoryNode> subdirectories = new TreeMap<>();
        List<SourceFileCoverageData> sourceFiles = new ArrayList<>();
        CoverageMetrics localMetrics = new CoverageMetrics();

        DirectoryNode(String name) {
            this.name = name;
        }

        CoverageMetrics aggregateMetrics() {
            CoverageMetrics aggregated = new CoverageMetrics();
            aggregated.add(localMetrics);
            for (DirectoryNode subdir : subdirectories.values()) {
                aggregated.add(subdir.aggregateMetrics());
            }
            return aggregated;
        }
    }

    static class SourceFileCoverageData {
        String fileName;
        CoverageMetrics metrics;

        SourceFileCoverageData(String fileName, CoverageMetrics metrics) {
            this.fileName = fileName;
            this.metrics = metrics;
        }
    }

    static class CoverageMetrics {
        int totalClasses, coveredClasses, totalMethods, coveredMethods, totalLines, coveredLines, totalBranches, coveredBranches;

        void add(CoverageMetrics other) {
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