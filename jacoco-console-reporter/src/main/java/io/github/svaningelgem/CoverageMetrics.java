package io.github.svaningelgem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Comprehensive collection of coverage metrics for a source file or directory.
 * Tracks coverage at multiple levels: classes, methods, lines, and branches.
 * All metrics maintain both total count and covered count for percentage calculation.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CoverageMetrics implements Cloneable {
    /**
     * Total number of classes in the scope
     */
    public int totalClasses;
    /**
     * Number of classes that have any coverage
     */
    public int coveredClasses;
    /**
     * Total number of methods across all classes
     */
    public int totalMethods;
    /**
     * Number of methods that have been executed
     */
    public int coveredMethods;
    /**
     * Total number of lines of code
     */
    public int totalLines;
    /**
     * Number of lines that have been executed
     */
    public int coveredLines;
    /**
     * Total number of branches in conditional statements
     */
    public int totalBranches;
    /**
     * Number of branches that have been executed
     */
    public int coveredBranches;

    void add(@NotNull CoverageMetrics other) {
        totalClasses += other.totalClasses;
        coveredClasses += other.coveredClasses;
        totalMethods += other.totalMethods;
        coveredMethods += other.coveredMethods;
        totalLines += other.totalLines;
        coveredLines += other.coveredLines;
        totalBranches += other.totalBranches;
        coveredBranches += other.coveredBranches;
    }

    @Override
    public CoverageMetrics clone() {
        return new CoverageMetrics(totalClasses, coveredClasses, totalMethods, coveredMethods, totalLines, coveredLines, totalBranches, coveredBranches);
    }
}
