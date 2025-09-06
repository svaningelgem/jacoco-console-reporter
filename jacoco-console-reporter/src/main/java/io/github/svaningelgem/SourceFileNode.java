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

    /**
     * Formatted string of missing lines (e.g., "33-35, 39")
     */
    private final String missingLines;

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public void printTree(@NotNull Log log, String prefix,
                          String format, String packagePath, boolean showFiles) {
        CoverageMetrics metrics = getMetrics();

        String output = String.format(format,
                Defaults.getInstance().truncateMiddle(prefix + getName()),
                Defaults.getInstance().formatCoverage(metrics.getCoveredClasses(), metrics.getTotalClasses()),
                Defaults.getInstance().formatCoverage(metrics.getCoveredMethods(), metrics.getTotalMethods()),
                Defaults.getInstance().formatCoverage(metrics.getCoveredBranches(), metrics.getTotalBranches()),
                Defaults.getInstance().formatCoverage(metrics.getCoveredLines(), metrics.getTotalLines()));

        // Append missing lines if available
        if (missingLines != null && !missingLines.isEmpty()) {
            output += "   Missing: " + missingLines;
        }

        log.info(output);
    }
}