package io.github.svaningelgem;

import lombok.Data;
import lombok.RequiredArgsConstructor;
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

    @Override
    public String getName() {
        return fileName;
    }
}