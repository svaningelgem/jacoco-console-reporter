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
/*
    @Data
    @AllArgsConstructor
    private static class Prefix implements Cloneable {
        String notLast = "";
        String last = "";

        @Contract(" -> new")
        @Override
        public @NotNull Prefix clone() {
            return new Prefix(notLast, last);
        }
    }

 */

    @Override
    public void printTree(org.apache.maven.plugin.logging.Log log, String prefix,
                          String format, String packagePath, boolean showFiles) {
        // Skip empty directories
        if (!shouldInclude()) {
            return;
        }

        packagePath = packagePath.replaceAll("^\\.", "");

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
/*
        // Determine if we need tree indicators at the first level
        Prefix current = new Prefix();
        if ( dirNodes.size() > 1 ) {
            current.notLast = Defaults.TEE;
            current.last = Defaults.CORNER;
        }

 */

        boolean useTreeForRoot = dirNodes.size() > 1 || !fileNodes.isEmpty();

        // Print directory nodes first
        for (int i = 0; i < dirNodes.size(); i++) {
            boolean isLast = (i == dirNodes.size() - 1) && fileNodes.isEmpty();
            DirectoryNode node = dirNodes.get(i);

            String connector = isLast ? Defaults.CORNER : Defaults.TEE;
            if (prefix.endsWith(Defaults.CORNER)) {
                prefix = prefix.substring(0, prefix.length() - Defaults.CORNER.length()) + Defaults.LASTDIR_SPACE;
            }
            else if (prefix.endsWith(Defaults.TEE)) {
                prefix = prefix.substring(0, prefix.length() - Defaults.TEE.length()) + Defaults.VERTICAL_LINE;
            }

            node.printTree(log, prefix + connector, format, packagePath, showFiles);
        }

        // Print source files after directories
        if (!fileNodes.isEmpty()) {
            // Calculate the prefix for files
            String filePrefix = isRoot ? "" : prefix.replace(Defaults.TEE, Defaults.VERTICAL_LINE).replace(Defaults.CORNER, Defaults.LASTDIR_SPACE);

            for (int i = 0; i < fileNodes.size(); i++) {
                boolean isLast = (i == fileNodes.size() - 1);
                SourceFileNode node = fileNodes.get(i);

                String connector = isLast ? Defaults.CORNER : Defaults.TEE;
                if (isRoot && useTreeForRoot) {
                    node.printTree(log, connector, format, packagePath, showFiles);
                } else {
                    node.printTree(log, filePrefix + connector, format, packagePath, showFiles);
                }
            }
        }
    }

    /**
     * Determines if a directory should be collapsed with its children
     * (i.e., it has exactly one subdirectory and no files)
     */
    private boolean shouldCollapseDirectory(DirectoryNode dir, boolean showFiles) {
        if (showFiles && !dir.getSourceFiles().isEmpty()) {
            return false;
        }

        if (dir.getSubdirectories().size() != 1) {
            return false;
        }

        DirectoryNode subdir = dir.getSubdirectories().values().iterator().next();
        return subdir.shouldInclude();
    }

    /**
     * Print a collapsed directory path (e.g., "com.example" instead of "com" -> "example")
     */
    private void printCollapsedPath(org.apache.maven.plugin.logging.Log log, @NotNull DirectoryNode dir,
                                    String prefix, boolean isLast, String format,
                                    String packagePath, boolean showFiles, boolean useTreeIndicator) {
        // Build the collapsed path string
        StringBuilder path = new StringBuilder(dir.getName());
        DirectoryNode current = dir;

        // Follow the chain of single subdirectories
        while (shouldCollapseDirectory(current, showFiles)) {
            DirectoryNode subdir = current.getSubdirectories().values().iterator().next();
            path.append(".").append(subdir.getName());
            current = subdir;
        }

        // Display the collapsed path as a node
        CoverageMetrics metrics = dir.getMetrics();
        String displayPath;
        if (useTreeIndicator) {
            displayPath = prefix + path.toString();
        } else {
            displayPath = path.toString();
        }

        log.info(String.format(format,
                Defaults.truncateMiddle(displayPath),
                Defaults.formatCoverage(metrics.getCoveredClasses(), metrics.getTotalClasses()),
                Defaults.formatCoverage(metrics.getCoveredMethods(), metrics.getTotalMethods()),
                Defaults.formatCoverage(metrics.getCoveredBranches(), metrics.getTotalBranches()),
                Defaults.formatCoverage(metrics.getCoveredLines(), metrics.getTotalLines())));

        // Calculate the full package path for children
        String fullPath = packagePath.isEmpty() ? path.toString() :
                packagePath + "." + path.toString();

        // Separate directories and files for consistent ordering
        List<DirectoryNode> childDirs = new ArrayList<>();
        List<SourceFileNode> childFiles = new ArrayList<>();

        // Get the contents of the last directory in the chain
        for (DirectoryNode subdir : current.getSubdirectories().values()) {
            if (subdir.shouldInclude()) {
                childDirs.add(subdir);
            }
        }

        if (showFiles) {
            childFiles.addAll(current.getSourceFiles());
        }

        // Sort the child nodes
        Collections.sort(childDirs);
        Collections.sort(childFiles);

        // Calculate the base prefix for children
        String basePrefixForChildren;
        if (useTreeIndicator) {
            basePrefixForChildren = isLast ? Defaults.LASTDIR_SPACE : Defaults.VERTICAL_LINE;
        } else {
            basePrefixForChildren = "  "; // No tree indicator at root level
        }

        // Print directories first
        for (int i = 0; i < childDirs.size(); i++) {
            boolean isLastDir = (i == childDirs.size() - 1) && childFiles.isEmpty();
            DirectoryNode node = childDirs.get(i);
            String childConnector = isLastDir ? Defaults.CORNER : Defaults.TEE;

            if (shouldCollapseDirectory(node, showFiles)) {
                printCollapsedPath(log, node, basePrefixForChildren, isLastDir, format,
                        fullPath, showFiles, true);
            } else {
                node.printTree(log, basePrefixForChildren + childConnector, format,
                        fullPath, showFiles);
            }
        }

        // Print files after directories
        for (int i = 0; i < childFiles.size(); i++) {
            boolean isLastFile = (i == childFiles.size() - 1);
            SourceFileNode node = childFiles.get(i);
            String childConnector = isLastFile ? Defaults.CORNER : Defaults.TEE;

            node.printTree(log, basePrefixForChildren + childConnector, format,
                    fullPath, showFiles);
        }
    }
}
