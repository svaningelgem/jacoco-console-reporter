package io.github.svaningelgem;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * A node representing a source file in the coverage tree.
 */
@Data
@RequiredArgsConstructor
class SourceFileNode implements FileSystemNode {
    /**
     * Name of the source file
     */
    private final String fileName;

    /**
     * Coverage metrics for this file
     */
    private final CoverageMetrics metrics;

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public boolean shouldInclude(boolean showFiles) {
        // Files are always included if showing files is enabled
        return showFiles;
    }

    @Override
    public void printTree(org.apache.maven.plugin.logging.Log log, String prefix,
                          String format, String packagePath, boolean showFiles) {
        if (!showFiles) {
            return;
        }

        log.info(String.format(format,
                Defaults.truncateMiddle(prefix + fileName),
                Defaults.formatCoverage(metrics.getCoveredClasses(), metrics.getTotalClasses()),
                Defaults.formatCoverage(metrics.getCoveredMethods(), metrics.getTotalMethods()),
                Defaults.formatCoverage(metrics.getCoveredBranches(), metrics.getTotalBranches()),
                Defaults.formatCoverage(metrics.getCoveredLines(), metrics.getTotalLines())));
    }
}