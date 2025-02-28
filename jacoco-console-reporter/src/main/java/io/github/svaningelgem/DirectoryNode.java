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
        String displayName = isRoot ? "" : prefix + name;
        String currentPath = isRoot ? packagePath : (packagePath.isEmpty() ? name : packagePath + "." + name);

        if (!isRoot) {
            log.info(String.format(format,
                    Defaults.truncateMiddle(displayName),
                    Defaults.formatCoverage(getMetrics().getCoveredClasses(), getMetrics().getTotalClasses()),
                    Defaults.formatCoverage(getMetrics().getCoveredMethods(), getMetrics().getTotalMethods()),
                    Defaults.formatCoverage(getMetrics().getCoveredBranches(), getMetrics().getTotalBranches()),
                    Defaults.formatCoverage(getMetrics().getCoveredLines(), getMetrics().getTotalLines())));
        }

        // Find nodes to be printed (skip empty directories)
        List<FileSystemNode> items = new ArrayList<>();

        // Add non-empty subdirectories
        for (DirectoryNode subdir : subdirectories.values()) {
            if (subdir.shouldInclude(showFiles)) {
                items.add(subdir);
            }
        }

        // Add files if showing files is enabled
        if (showFiles) {
            items.addAll(sourceFiles);
        }

        // Sort items (using the default compareTo method)
        Collections.sort(items);

        // Calculate base prefix for children
        String basePrefix = isRoot ? "" : prefix + " ";

        // Process each item
        for (int i = 0; i < items.size(); i++) {
            FileSystemNode item = items.get(i);
            boolean isLast = i == items.size() - 1;

            // Choose the appropriate tree connector
            String connector = isLast ? Defaults.CORNER : Defaults.TEE;
            String childIndent = isLast ? Defaults.LASTDIR_SPACE : Defaults.VERTICAL_LINE;

            if (isRoot) {
                // Special case for root's direct children
                if (item instanceof DirectoryNode) {
                    DirectoryNode dir = (DirectoryNode) item;

                    // Check if we should collapse this directory path
                    if (shouldCollapseDirectory(dir, showFiles)) {
                        // Collapse the directory path
                        printCollapsedDirectoryPath(log, dir, basePrefix, connector, format,
                                currentPath, showFiles);
                    } else {
                        // Print normally
                        String rootItemPrefix = connector; // For root's direct children
                        item.printTree(log, rootItemPrefix, format,
                                currentPath.isEmpty() ? item.getName() :
                                        currentPath + "." + item.getName(),
                                showFiles);
                    }
                } else {
                    // Root's direct file child - should not normally happen
                    item.printTree(log, "", format, currentPath, showFiles);
                }
            } else {
                String itemPrefix = basePrefix + connector;

                if (item instanceof DirectoryNode) {
                    // Determine if this directory should be collapsed (single path to display)
                    DirectoryNode dir = (DirectoryNode) item;

                    // Check if we should collapse this directory
                    if (shouldCollapseDirectory(dir, showFiles)) {
                        // Collapse the directory path
                        printCollapsedDirectoryPath(log, dir, basePrefix, connector, format,
                                currentPath, showFiles);
                    } else {
                        // Print normally
                        item.printTree(log, itemPrefix, format,
                                currentPath.isEmpty() ? item.getName() :
                                        currentPath + "." + item.getName(),
                                showFiles);
                    }
                } else {
                    // File node - print normally
                    item.printTree(log, itemPrefix, format, currentPath, showFiles);
                }
            }
        }
    }

    /**
     * Determine if a directory path should be collapsed (single path with no files)
     */
    private boolean shouldCollapseDirectory(DirectoryNode dir, boolean showFiles) {
        // Only collapse if it has exactly one subdirectory and no files
        if (showFiles && !dir.getSourceFiles().isEmpty()) {
            return false;
        }

        if (dir.getSubdirectories().size() != 1) {
            return false;
        }

        // Get the single subdirectory
        DirectoryNode subdir = dir.getSubdirectories().values().iterator().next();

        // Check if that subdirectory should be included
        return subdir.shouldInclude(showFiles);
    }

    /**
     * Print a collapsed directory path (e.g., "com.example.util" instead of separate nodes)
     */
    private void printCollapsedDirectoryPath(org.apache.maven.plugin.logging.Log log,
                                             DirectoryNode dir, String prefix, String connector,
                                             String format, String packagePath, boolean showFiles) {
        // Build the collapsed path
        StringBuilder collapsedPath = new StringBuilder(dir.getName());
        DirectoryNode current = dir;

        while (shouldCollapseDirectory(current, showFiles)) {
            DirectoryNode subdir = current.getSubdirectories().values().iterator().next();
            collapsedPath.append(".").append(subdir.getName());
            current = subdir;
        }

        // Print the collapsed path
        CoverageMetrics metrics = dir.getMetrics();
        String fullPath = packagePath.isEmpty() ? collapsedPath.toString() :
                packagePath + "." + collapsedPath.toString();

        log.info(String.format(format,
                Defaults.truncateMiddle(prefix + connector + collapsedPath),
                Defaults.formatCoverage(metrics.getCoveredClasses(), metrics.getTotalClasses()),
                Defaults.formatCoverage(metrics.getCoveredMethods(), metrics.getTotalMethods()),
                Defaults.formatCoverage(metrics.getCoveredBranches(), metrics.getTotalBranches()),
                Defaults.formatCoverage(metrics.getCoveredLines(), metrics.getTotalLines())));

        // Get the last directory in the chain
        DirectoryNode lastDir = current;

        // Process the children of the last directory in the chain
        String childPrefix = prefix + (connector.equals(Defaults.CORNER) ? " " : Defaults.VERTICAL_LINE);

        // Find nodes to be printed in the last directory
        List<FileSystemNode> items = new ArrayList<>();

        // Add non-empty subdirectories
        for (DirectoryNode subdir : lastDir.getSubdirectories().values()) {
            if (subdir.shouldInclude(showFiles)) {
                items.add(subdir);
            }
        }

        // Add files if showing files is enabled
        if (showFiles) {
            items.addAll(lastDir.getSourceFiles());
        }

        // Sort items
        Collections.sort(items);

        // Process each item in the last directory
        for (int i = 0; i < items.size(); i++) {
            FileSystemNode item = items.get(i);
            boolean isLast = i == items.size() - 1;

            String itemConnector = isLast ? Defaults.CORNER : Defaults.TEE;

            if (item instanceof DirectoryNode) {
                item.printTree(log, childPrefix + itemConnector, format, fullPath, showFiles);
            } else {
                // File node
                item.printTree(log, childPrefix + itemConnector, format, fullPath, showFiles);
            }
        }
    }
}