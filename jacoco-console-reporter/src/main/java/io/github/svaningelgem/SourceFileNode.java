package io.github.svaningelgem;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.logging.Log;
import org.jetbrains.annotations.NotNull;

/**
 * A node representing a source file in the coverage tree.
 */
@Data
@RequiredArgsConstructor
public class SourceFileNode implements FileSystemNode {
    /**
     * Name of the source file
     */
    private final String fileName;

    /**
     * Coverage metrics for this file
     */
    private final CoverageMetrics metrics;

    private final String missingLines;

    public SourceFileNode(String fileName, CoverageMetrics metrics) {
        this(fileName, metrics, "");
    }

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public void printTree(@NotNull Log log, String prefix, @NotNull String format, String packagePath, boolean showFiles) {
        CoverageMetrics metrics = getMetrics();

        if (format.contains("Missing")) {
            // Use format with missing lines
            log.info(String.format(format,
                    Defaults.getInstance().truncateMiddleForMissing(prefix + getName()),
                    Defaults.getInstance().formatCoverage(metrics.getCoveredClasses(), metrics.getTotalClasses()),
                    Defaults.getInstance().formatCoverage(metrics.getCoveredMethods(), metrics.getTotalMethods()),
                    Defaults.getInstance().formatCoverage(metrics.getCoveredBranches(), metrics.getTotalBranches()),
                    Defaults.getInstance().formatCoverage(metrics.getCoveredLines(), metrics.getTotalLines()),
                    Defaults.getInstance().truncateMissing(missingLines)));
        } else {
            // Use original format
            log.info(String.format(format,
                    Defaults.getInstance().truncateMiddle(prefix + getName()),
                    Defaults.getInstance().formatCoverage(metrics.getCoveredClasses(), metrics.getTotalClasses()),
                    Defaults.getInstance().formatCoverage(metrics.getCoveredMethods(), metrics.getTotalMethods()),
                    Defaults.getInstance().formatCoverage(metrics.getCoveredBranches(), metrics.getTotalBranches()),
                    Defaults.getInstance().formatCoverage(metrics.getCoveredLines(), metrics.getTotalLines())));
        }
    }
}