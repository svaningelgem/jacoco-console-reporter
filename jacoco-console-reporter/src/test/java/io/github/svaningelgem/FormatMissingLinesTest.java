package io.github.svaningelgem;

import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FormatMissingLinesTest extends BaseTestClass {

    private @NotNull ISourceFileCoverage createCoverage(int first, int last, Set<Integer> missing) {
        ISourceFileCoverage cov = mock(ISourceFileCoverage.class);
        when(cov.getFirstLine()).thenReturn(first);
        when(cov.getLastLine()).thenReturn(last);
        when(cov.getLine(anyInt())).thenAnswer(inv -> {
            int lineNum = inv.getArgument(0);
            ILine line = mock(ILine.class);
            int status = missing.contains(lineNum) ? ICounter.NOT_COVERED : ICounter.FULLY_COVERED;
            when(line.getStatus()).thenReturn(status);
            return line;
        });
        return cov;
    }

    @Test
    public void noMissingLines() {
        ISourceFileCoverage cov = createCoverage(1, 5, Collections.emptySet());
        assertTrue(mojo.formatMissingLines(cov).isEmpty());
    }

    @Test
    public void singleMissingLine() {
        ISourceFileCoverage cov = createCoverage(1, 5, new HashSet<>(Collections.singletonList(3)));
        assertEquals("3", mojo.formatMissingLines(cov));
    }

    @Test
    public void twoConsecutive() {
        ISourceFileCoverage cov = createCoverage(1, 5, new HashSet<>(Arrays.asList(2, 3)));
        assertEquals("2-3", mojo.formatMissingLines(cov));
    }

    @Test
    public void twoNonConsecutive() {
        ISourceFileCoverage cov = createCoverage(1, 5, new HashSet<>(Arrays.asList(1, 5)));
        assertEquals("1, 5", mojo.formatMissingLines(cov));
    }

    @Test
    public void multipleRangesAndSingles() {
        ISourceFileCoverage cov = createCoverage(1, 10, new HashSet<>(Arrays.asList(1, 2, 3, 5, 7, 8, 9, 10)));
        assertEquals("1-3, 5, 7-10", mojo.formatMissingLines(cov));
    }

    @Test
    public void allMissing() {
        ISourceFileCoverage cov = createCoverage(1, 4, new HashSet<>(Arrays.asList(1, 2, 3, 4)));
        assertEquals("1-4", mojo.formatMissingLines(cov));
    }

    @Test
    public void singleLineFileMissing() {
        ISourceFileCoverage cov = createCoverage(1, 1, new HashSet<>(Collections.singletonList(1)));
        assertEquals("1", mojo.formatMissingLines(cov));
    }

    @Test
    public void singleLineFileCovered() {
        ISourceFileCoverage cov = createCoverage(1, 1, Collections.emptySet());
        assertTrue(mojo.formatMissingLines(cov).isEmpty());
    }

    @Test
    public void missingWithGaps() {
        ISourceFileCoverage cov = createCoverage(10, 20, new HashSet<>(Arrays.asList(10, 12, 14, 15, 16, 20)));
        assertEquals("10, 12, 14-16, 20", mojo.formatMissingLines(cov));
    }

    @Test
    public void noLines() {
        ISourceFileCoverage cov = createCoverage(1, 0, Collections.emptySet());
        assertTrue(mojo.formatMissingLines(cov).isEmpty());
    }
}