package io.github.svaningelgem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A node representing a directory (package) in the coverage tree.
 */
@Data
@RequiredArgsConstructor
public class DirectoryNode implements FileSystemNode {
    /**
     * Name of the directory/package component
     */
    public final String name;

    /**
     * Map of subdirectories, keyed by name for easy tree construction
     */
    public final Map<String, DirectoryNode> subdirectories = new TreeMap<>();

    /**
     * List of source files in this directory, with their coverage metrics
     */
    public final List<SourceFileNode> sourceFiles = new ArrayList<>();

    @Override
    public CoverageMetrics getMetrics() {
        return aggregateMetrics();
    }

    /**
     * Aggregate metrics from this directory's files and subdirectories
     */
    CoverageMetrics aggregateMetrics() {
        CoverageMetrics aggregated = new CoverageMetrics();
        sourceFiles.forEach(file -> aggregated.add(file.getMetrics()));
        subdirectories.values().forEach(subdir -> aggregated.add(subdir.aggregateMetrics()));
        return aggregated;
    }

    public boolean shouldInclude() {
        return !sourceFiles.isEmpty() || subdirectories.values().stream().anyMatch(DirectoryNode::shouldInclude);
    }

    private <T extends FileSystemNode> void printNodes(org.apache.maven.plugin.logging.Log log, String prefix,
                            String format, String packagePath, boolean showFiles, @NotNull List<T> nodes, boolean extraCheck) {
        for (int i = 0; i < nodes.size(); i++) {
            boolean isLast = (i == nodes.size() - 1) && extraCheck;
            FileSystemNode node = nodes.get(i);

            node.printTree(log, determineNewPrefix(prefix, isLast), format, packagePath, showFiles);
        }
    }

    private @NotNull String determineNewPrefix(@NotNull String oldPrefix, boolean isLast) {
        String prefix = oldPrefix;

        if (prefix.endsWith(Defaults.CORNER)) {
            prefix = prefix.substring(0, prefix.length() - Defaults.CORNER.length()) + Defaults.LASTDIR_SPACE;
        }
        else if (prefix.endsWith(Defaults.TEE)) {
            prefix = prefix.substring(0, prefix.length() - Defaults.TEE.length()) + Defaults.VERTICAL_LINE;
        }

        String connector = isLast ? Defaults.CORNER : Defaults.TEE;
        return prefix + connector;
    }

    @Override
    public void printTree(org.apache.maven.plugin.logging.Log log, String prefix,
                          String format, String packagePath, boolean showFiles) {
        // Skip empty directories
        if (!shouldInclude()) {
            return;
        }

        packagePath = packagePath.replaceAll("^\\.", ""); // ltrim('.')

        // Skip printing the empty root node
        final boolean isRoot = name.isEmpty();

        // Collect directory nodes and file nodes separately
        List<DirectoryNode> dirNodes = subdirectories.values().stream().filter(DirectoryNode::shouldInclude).sorted().collect(Collectors.toList());
        List<SourceFileNode> fileNodes = showFiles ? sourceFiles.stream().sorted().collect(Collectors.toList()) : new ArrayList<>();

        boolean shouldCollapse = dirNodes.size() == 1 && fileNodes.isEmpty();
        if (shouldCollapse) {
            DirectoryNode onlyNode = dirNodes.get(0);
            onlyNode.printTree(log, prefix, format, packagePath + "." + getName(), showFiles);
            return;
        }

        String printableName = isRoot ? "<root>" : prefix + packagePath + (packagePath.isEmpty() ? "" : ".") + name;
        log.info(String.format(format,
                Defaults.truncateMiddle(printableName),
                Defaults.formatCoverage(getMetrics().getCoveredClasses(), getMetrics().getTotalClasses()),
                Defaults.formatCoverage(getMetrics().getCoveredMethods(), getMetrics().getTotalMethods()),
                Defaults.formatCoverage(getMetrics().getCoveredBranches(), getMetrics().getTotalBranches()),
                Defaults.formatCoverage(getMetrics().getCoveredLines(), getMetrics().getTotalLines())));

        packagePath = "";  // Reset because we shouldn't collapse now anymore

        // Print directory nodes first
        printNodes(log, prefix, format, packagePath, showFiles, dirNodes, fileNodes.isEmpty());
        // Then files
        printNodes(log, prefix, format, packagePath, showFiles, fileNodes, true);
    }
}
