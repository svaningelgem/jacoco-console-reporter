package io.github.svaningelgem;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * A node representing a directory (package) in the coverage tree.
 */
@Data
@RequiredArgsConstructor
class DirectoryNode implements FileSystemNode {
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

    @Override
    public boolean shouldInclude(boolean showFiles) {
        // Check if this directory has any files (when showing files)
        if (showFiles && !sourceFiles.isEmpty()) {
            return true;
        }

        // Check if it has any subdirectories that should be included
        for (DirectoryNode subdir : subdirectories.values()) {
            if (subdir.shouldInclude(showFiles)) {
                return true;
            }
        }

        // Empty directory - skip it
        return false;
    }

    @Override
    public void printTree(org.apache.maven.plugin.logging.Log log, String prefix,
                          String format, String packagePath, boolean showFiles) {
        // Skip empty directories
        if (!shouldInclude(showFiles)) {
            return;
        }

        // Skip printing the empty root node
        boolean isRoot = name.isEmpty();
        String currentPath = isRoot ? packagePath :
                (packagePath.isEmpty() ? name : packagePath + "." + name);

        if (!isRoot) {
            log.info(String.format(format,
                    Defaults.truncateMiddle(prefix + name),
                    Defaults.formatCoverage(getMetrics().getCoveredClasses(), getMetrics().getTotalClasses()),
                    Defaults.formatCoverage(getMetrics().getCoveredMethods(), getMetrics().getTotalMethods()),
                    Defaults.formatCoverage(getMetrics().getCoveredBranches(), getMetrics().getTotalBranches()),
                    Defaults.formatCoverage(getMetrics().getCoveredLines(), getMetrics().getTotalLines())));
        }

        // Collect nodes that should be printed
        List<FileSystemNode> nodes = new ArrayList<>();

        // Add subdirectories
        for (DirectoryNode subdir : subdirectories.values()) {
            if (subdir.shouldInclude(showFiles)) {
                nodes.add(subdir);
            }
        }

        // Add source files if needed
        if (showFiles) {
            nodes.addAll(sourceFiles);
        }

        // Sort nodes
        Collections.sort(nodes);

        // Print child nodes
        String childPrefix = prefix;
        for (int i = 0; i < nodes.size(); i++) {
            boolean isLast = (i == nodes.size() - 1);
            FileSystemNode node = nodes.get(i);

            if (isRoot) {
                // Handle collapsible directories at root level
                if (node instanceof DirectoryNode && shouldCollapseDirectory((DirectoryNode) node, showFiles)) {
                    printCollapsedPath(log, (DirectoryNode) node, "", isLast, format, currentPath, showFiles);
                } else {
                    // Normal node at root level
                    String connector = isLast ? Defaults.CORNER : Defaults.TEE;
                    node.printTree(log, connector, format, currentPath, showFiles);
                }
            } else {
                // Non-root nodes
                String connector = isLast ? Defaults.CORNER : Defaults.TEE;
                String nextPrefix = childPrefix;

                // Handle collapsible directories
                if (node instanceof DirectoryNode && shouldCollapseDirectory((DirectoryNode) node, showFiles)) {
                    printCollapsedPath(log, (DirectoryNode) node, childPrefix, isLast, format, currentPath, showFiles);
                } else {
                    // Print the node normally
                    node.printTree(log, nextPrefix + connector, format, currentPath, showFiles);
                }
            }

            // Update the child prefix for subsequent siblings
            if (!isRoot) {
                childPrefix = prefix;
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
        return subdir.shouldInclude(showFiles);
    }

    /**
     * Print a collapsed directory path (e.g., "com.example" instead of "com" -> "example")
     */
    private void printCollapsedPath(org.apache.maven.plugin.logging.Log log, DirectoryNode dir,
                                    String prefix, boolean isLast, String format,
                                    String packagePath, boolean showFiles) {
        // Build the collapsed path string
        StringBuilder path = new StringBuilder(dir.getName());
        DirectoryNode current = dir;

        // Follow the chain of single subdirectories
        while (shouldCollapseDirectory(current, showFiles)) {
            DirectoryNode subdir = current.getSubdirectories().values().iterator().next();
            path.append(".").append(subdir.getName());
            current = subdir;
        }

        // Use the appropriate connector based on whether this is the last item
        String connector = isLast ? Defaults.CORNER : Defaults.TEE;
        String displayPath = prefix + connector + path.toString();

        // Print the collapsed path as a node
        CoverageMetrics metrics = dir.getMetrics();
        log.info(String.format(format,
                Defaults.truncateMiddle(displayPath),
                Defaults.formatCoverage(metrics.getCoveredClasses(), metrics.getTotalClasses()),
                Defaults.formatCoverage(metrics.getCoveredMethods(), metrics.getTotalMethods()),
                Defaults.formatCoverage(metrics.getCoveredBranches(), metrics.getTotalBranches()),
                Defaults.formatCoverage(metrics.getCoveredLines(), metrics.getTotalLines())));

        // Calculate the full package path for children
        String fullPath = packagePath.isEmpty() ? path.toString() :
                packagePath + "." + path.toString();

        // Get the contents of the last directory in the chain
        List<FileSystemNode> childNodes = new ArrayList<>();

        if (showFiles) {
            childNodes.addAll(current.getSourceFiles());
        }

        for (DirectoryNode subdir : current.getSubdirectories().values()) {
            if (subdir.shouldInclude(showFiles)) {
                childNodes.add(subdir);
            }
        }

        // Sort the child nodes
        Collections.sort(childNodes);

        // Calculate the prefix for children
        String childPrefix = prefix + (isLast ? Defaults.LASTDIR_SPACE : Defaults.VERTICAL_LINE) + " ";

        // Print each child
        for (int i = 0; i < childNodes.size(); i++) {
            boolean isLastChild = (i == childNodes.size() - 1);
            FileSystemNode node = childNodes.get(i);
            String childConnector = isLastChild ? Defaults.CORNER : Defaults.TEE;

            if (node instanceof DirectoryNode && shouldCollapseDirectory((DirectoryNode) node, showFiles)) {
                printCollapsedPath(log, (DirectoryNode) node, childPrefix, isLastChild, format, fullPath, showFiles);
            } else {
                node.printTree(log, childPrefix + childConnector, format, fullPath, showFiles);
            }
        }
    }
}