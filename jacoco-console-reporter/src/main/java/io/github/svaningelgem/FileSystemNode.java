package io.github.svaningelgem;

import org.jetbrains.annotations.NotNull;

/**
 * Common interface for file system nodes in the coverage tree.
 * Provides methods that both directories and files need to implement.
 */
interface FileSystemNode extends Comparable<FileSystemNode> {
    /**
     * Get the name of this node (directory name or file name)
     */
    String getName();

    /**
     * Get the coverage metrics for this node
     */
    CoverageMetrics getMetrics();

    /**
     * Print this node's tree representation and its children
     *
     * @param log          The logger to use for output
     * @param prefix       Current prefix for tree visualization
     * @param format       Format string for output
     * @param packagePath  Current package path
     * @param showFiles    Whether to show files
     */
    void printTree(org.apache.maven.plugin.logging.Log log, String prefix,
                   String format, String packagePath, boolean showFiles);

    /**
     * Default comparison implementation - directories come before files,
     * then sort alphabetically within each type
     */
    @Override
    default int compareTo(@NotNull FileSystemNode other) {
        // Sort by type (directories before files)
        boolean thisIsDir = this instanceof DirectoryNode;
        boolean otherIsDir = other instanceof DirectoryNode;

        if (thisIsDir && !otherIsDir) return -1;
        if (!thisIsDir && otherIsDir) return 1;

        // Sort alphabetically within the same type
        return this.getName().compareToIgnoreCase(other.getName());
    }
}