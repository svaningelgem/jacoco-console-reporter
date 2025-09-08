package io.github.svaningelgem;

import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildDirectoryTreeExclusionTest extends BaseTestClass {
    @Test
    public void testEmptyExclusionWithClassFileSuffix() {
        mojo.addExclusion(".Class");
        assertTrue(JacocoConsoleReporterMojo.collectedExcludePatterns.isEmpty());
    }

    @Test
    public void testAllPossibleExclusions() throws Exception {
        // Configure project with our directories
        configureProjectForTesting(null);

        // Execute to set up targetDir and baseDir in mojo
        mojo.execute();

        // Create Java source files with package declarations for our test
        createFile("classes/com/example/ExcludedClass.java",
                "package com.example;\npublic class ExcludedClass {}");
        createFile("classes/com/example/IncludedClass.java",
                "package com.example;\npublic class IncludedClass {}");

        // Create a pattern that will exclude ExcludedClass
        mojo.addExclusion("com/example/ExcludedClass");

        // Create a root directory node
        DirectoryNode root = new DirectoryNode("");

        // Create mocks for package, source file, and class coverage
        IPackageCoverage packageCoverage = createMockPackageCoverage("com/example");

        // Add source files to the package - one excluded, one included
        List<String> fileNames = Arrays.asList("ExcludedClass.java", "IncludedClass.java");
        setupMockSourceFiles(packageCoverage, fileNames);

        // Test the buildDirectoryTreeAddNode method
        for (String fileName : fileNames) {
            ISourceFileCoverage sourceCoverage = createMockSourceFileCoverage(fileName);
            mojo.buildDirectoryTreeAddNode(root, packageCoverage, sourceCoverage);
        }
    }

    @Test
    public void testBuildDirectoryTreeWithExcludedFiles() throws Exception {
        // Configure project with our directories
        configureProjectForTesting(null);

        // Execute to set up targetDir and baseDir in mojo
        mojo.execute();

        // Create Java source files with package declarations for our test
        createFile("classes/com/example/ExcludedClass.java",
                "package com.example;\npublic class ExcludedClass {}");
        createFile("classes/com/example/IncludedClass.java",
                "package com.example;\npublic class IncludedClass {}");

        // Create a pattern that will exclude ExcludedClass
        mojo.addExclusion("com/example/ExcludedClass");

        // Create a root directory node
        DirectoryNode root = new DirectoryNode("");

        // Create mocks for package, source file, and class coverage
        IPackageCoverage packageCoverage = createMockPackageCoverage("com/example");

        // Add source files to the package - one excluded, one included
        List<String> fileNames = Arrays.asList("ExcludedClass.java", "IncludedClass.java");
        setupMockSourceFiles(packageCoverage, fileNames);

        // Test the buildDirectoryTreeAddNode method
        for (String fileName : fileNames) {
            ISourceFileCoverage sourceCoverage = createMockSourceFileCoverage(fileName);
            mojo.buildDirectoryTreeAddNode(root, packageCoverage, sourceCoverage);
        }

        // Verify the tree structure
        assertTrue("com node should exist", root.getSubdirectories().containsKey("com"));
        DirectoryNode comNode = root.getSubdirectories().get("com");

        assertTrue("example node should exist", comNode.getSubdirectories().containsKey("example"));
        DirectoryNode exampleNode = comNode.getSubdirectories().get("example");

        // Verify that only IncludedClass.java is in the tree
        assertEquals("Only one file should be included", 1, exampleNode.getSourceFiles().size());
        assertEquals("IncludedClass.java should be included",
                "IncludedClass.java", exampleNode.getSourceFiles().get(0).getName());

        // Enable showing files in the report
        mojo.showFiles = true;

        // Print the tree to the log
        mojo.printTree(root);

        // Check that only the included file appears in the log
        String[] expected = {
                "[info] com.example        ",
                "[info] └─IncludedClass.java"
        };
        String[] notExpected = {
                "[info] └─ExcludedClass.java"
        };

        assertLogContains(expected);
        assertLogNotContains(notExpected);
    }

    /**
     * Helper method to create a mock package coverage
     */
    private @NotNull IPackageCoverage createMockPackageCoverage(String packageName) {
        IPackageCoverage packageCoverage = mock(IPackageCoverage.class);
        lenient().when(packageCoverage.getName()).thenReturn(packageName);
        return packageCoverage;
    }

    // Helper to create counter
    ICounter createCounter(int total, int covered) {
        ICounter counter = mock(ICounter.class);
        lenient().when(counter.getTotalCount()).thenReturn(total);
        lenient().when(counter.getCoveredCount()).thenReturn(covered);
        return counter;
    }

    // Helper to get counter status
    int getCounterStatus(@NotNull ICounter c) {
        int total = c.getTotalCount();
        int covered = c.getCoveredCount();
        if (total == 0) return ICounter.EMPTY;
        if (covered == 0) return ICounter.NOT_COVERED;
        if (covered == total) return ICounter.FULLY_COVERED;
        return ICounter.PARTLY_COVERED;
    }

    // Helper to compute line status
    int computeStatus(ICounter instr, ICounter br) {
        int statusI = getCounterStatus(instr);
        int statusB = getCounterStatus(br);
        if (statusI == ICounter.EMPTY) {
            return statusB;
        } else if (statusB == ICounter.EMPTY) {
            return statusI;
        } else {
            return Math.min(statusI, statusB);
        }
    }

    // Setup helper
    void setupLine(@NotNull ILine line, int instrTotal, int instrCovered, int branchTotal, int branchCovered) {
        ICounter instrCounter = createCounter(instrTotal, instrCovered);
        ICounter branchCounter = createCounter(branchTotal, branchCovered);

        // Calculate status directly from values to avoid calling mocked methods during stubbing
        int instrStatus = calculateStatus(instrTotal, instrCovered);
        int branchStatus = calculateStatus(branchTotal, branchCovered);
        int finalStatus = combineStatuses(instrStatus, branchStatus);

        lenient().when(line.getInstructionCounter()).thenReturn(instrCounter);
        lenient().when(line.getBranchCounter()).thenReturn(branchCounter);
        lenient().when(line.getStatus()).thenReturn(finalStatus);
    }

    private int calculateStatus(int total, int covered) {
        if (total == 0) return ICounter.EMPTY;
        if (covered == 0) return ICounter.NOT_COVERED;
        if (covered == total) return ICounter.FULLY_COVERED;
        return ICounter.PARTLY_COVERED;
    }

    private int combineStatuses(int instrStatus, int branchStatus) {
        if (instrStatus == ICounter.EMPTY) {
            return branchStatus;
        } else if (branchStatus == ICounter.EMPTY) {
            return instrStatus;
        } else {
            return Math.min(instrStatus, branchStatus);
        }
    }

    /**
     * Helper method to create a mock source file coverage
     */
    private @NotNull ISourceFileCoverage createMockSourceFileCoverage(String fileName) {
        ISourceFileCoverage sourceCoverage = mock(ISourceFileCoverage.class);
        lenient().when(sourceCoverage.getName()).thenReturn(fileName);

        // Mock the line range - crucial for iteration
        lenient().when(sourceCoverage.getFirstLine()).thenReturn(1);
        lenient().when(sourceCoverage.getLastLine()).thenReturn(15);

        // Mock getLine to handle any line number - never return null to avoid NPE
        lenient().when(sourceCoverage.getLine(anyInt())).thenAnswer(invocation -> {
            int lineNr = invocation.getArgument(0);

            // Always return a valid line mock, even for invalid line numbers
            ILine line = mock(ILine.class);

            if (lineNr == 1) {
                // Line 1: single missing line
                setupLine(line, 1, 0, 0, 0);
            } else if (lineNr == 2) {
                // Line 2: separator - fully covered
                setupLine(line, 2, 2, 1, 1);
            } else if (lineNr >= 3 && lineNr <= 4) {
                // Lines 3-4: 2 consecutive missing lines
                setupLine(line, 1, 0, 0, 0);
            } else if (lineNr == 5) {
                // Line 5: separator - fully covered
                setupLine(line, 2, 2, 1, 1);
            } else if (lineNr >= 6 && lineNr <= 8) {
                // Lines 6-8: 3 consecutive missing lines
                setupLine(line, 1, 0, 0, 0);
            } else if (lineNr >= 9 && lineNr <= 11) {
                // Lines 9-11: 3 fully covered lines
                setupLine(line, 2, 2, 1, 1);
            } else if (lineNr == 12) {
                // Line 12: 1 partially covered line
                setupLine(line, 2, 1, 2, 1);
            } else if (lineNr == 13) {
                // Line 13: separator - fully covered
                setupLine(line, 2, 2, 1, 1);
            } else if (lineNr == 14) {
                // Line 14: another partially covered line
                setupLine(line, 2, 1, 2, 1);
            } else if (lineNr == 15) {
                // Line 15: single missing line
                setupLine(line, 1, 0, 0, 0);
            } else {
                // Other lines: empty (no executable code)
                setupLine(line, 0, 0, 0, 0);
            }

            return line;
        });

        // Mock counters for lines and branches
        ICounter lineCounter = mock(ICounter.class);
        lenient().when(lineCounter.getTotalCount()).thenReturn(15);
        lenient().when(lineCounter.getCoveredCount()).thenReturn(7); // Lines 2, 5, 9-11, 13 are fully covered
        ICounter branchCounter = mock(ICounter.class);
        lenient().when(branchCounter.getTotalCount()).thenReturn(8);
        lenient().when(branchCounter.getCoveredCount()).thenReturn(6);
        lenient().when(sourceCoverage.getLineCounter()).thenReturn(lineCounter);
        lenient().when(sourceCoverage.getBranchCounter()).thenReturn(branchCounter);

        return sourceCoverage;
    }

    /**
     * Helper method to create mock class coverages for a set of source files
     */
    private void setupMockSourceFiles(IPackageCoverage packageCoverage, @NotNull List<String> fileNames) {
        List<ISourceFileCoverage> sourceFiles = new ArrayList<>();
        List<IClassCoverage> classes = new ArrayList<>();

        for (String fileName : fileNames) {
            // Create source file coverage mock
            ISourceFileCoverage sourceCoverage = createMockSourceFileCoverage(fileName);
            sourceFiles.add(sourceCoverage);

            // Create class coverage mock
            IClassCoverage classCoverage = mock(IClassCoverage.class);
            when(classCoverage.getSourceFileName()).thenReturn(fileName);

            ICounter methodCounter = mock(ICounter.class);
            when(methodCounter.getTotalCount()).thenReturn(5);
            when(methodCounter.getCoveredCount()).thenReturn(4);

            when(classCoverage.getMethodCounter()).thenReturn(methodCounter);
            classes.add(classCoverage);
        }

        // Set up the package to return our source files and classes
        when(packageCoverage.getSourceFiles()).thenReturn(sourceFiles);
        when(packageCoverage.getClasses()).thenReturn(classes);
    }

    void setupMissingCoverage() {
        // Create a root directory node
        DirectoryNode root = new DirectoryNode("");

        // Create mocks for package, source file, and class coverage
        IPackageCoverage packageCoverage = createMockPackageCoverage("com/example");

        // Add source files to the package - one excluded, one included
        List<String> fileNames = Collections.singletonList("IncludedClass.java");
        setupMockSourceFiles(packageCoverage, fileNames);

        // Test the buildDirectoryTreeAddNode method
        for (String fileName : fileNames) {
            ISourceFileCoverage sourceCoverage = createMockSourceFileCoverage(fileName);
            mojo.buildDirectoryTreeAddNode(root, packageCoverage, sourceCoverage);
        }
        mojo.printCoverageReport(root);
    }

    @Test
    public void testCoverMissingInAddingDirTree() {
        mojo.showFiles = true;
        mojo.showMissingLines = true;

        setupMissingCoverage();

        String[] expected = {
                "[info] └─IncludedClass.java                               | 100.00% (1/1)        | 80.00% (4/5)         | 75.00% (6/8)         | 46.67% (7/15)          Missing: 1, 3-4, 6-8, 15, partial: 12, 14"
        };
        assertLogContains(expected, true);
    }

    @Test
    public void testCoverMissingInAddingDirTreeNoShowFiles() {
        mojo.showFiles = false;
        mojo.showMissingLines = true;

        setupMissingCoverage();

        String[] expected = {
                "[info] com.example                                        | 100.00% (1/1)        | 80.00% (4/5)         | 75.00% (6/8)         | 46.67% (7/15)       "
        };
        assertLogContains(expected, true);

        String[] notExpected = {
                "[info] └─IncludedClass.java"
        };
        assertLogNotContains(notExpected, false);
    }

    @Test
    public void testCoverMissingInAddingDirTreeShowFilesNoMissingLines() {
        mojo.showFiles = true;
        mojo.showMissingLines = false;

        setupMissingCoverage();

        String[] expected = {
                "[info] com.example                                        | 100.00% (1/1)        | 80.00% (4/5)         | 75.00% (6/8)         | 46.67% (7/15)       ",
                "[info] └─IncludedClass.java                               | 100.00% (1/1)        | 80.00% (4/5)         | 75.00% (6/8)         | 46.67% (7/15)       "
        };
        assertLogContains(expected, true);

        String[] notExpected = {
                "[info] └─IncludedClass.java                               | 100.00% (1/1)        | 80.00% (4/5)         | 75.00% (6/8)         | 46.67% (7/15)          Missing: 1, 3-4, 6-8, 15, partial: 12, 14"
        };
        assertLogNotContains(notExpected, true);
    }

    @Test
    public void testCoverMissingInAddingDirTreeNoShowFilesNoMissingLines() {
        mojo.showFiles = false;
        mojo.showMissingLines = false;

        setupMissingCoverage();

        String[] expected = {
                "[info] com.example                                        | 100.00% (1/1)        | 80.00% (4/5)         | 75.00% (6/8)         | 46.67% (7/15)       "
        };
        assertLogContains(expected, true);

        String[] notExpected = {
                "[info] └─IncludedClass.java"
        };
        assertLogNotContains(notExpected, false);
    }

    @Test
    public void testFormatMissingLinesOnlyPartial() {
        // Create a source file with only partial coverage (no missing lines)
        ISourceFileCoverage sourceCoverage = mock(ISourceFileCoverage.class);
        lenient().when(sourceCoverage.getFirstLine()).thenReturn(1);
        lenient().when(sourceCoverage.getLastLine()).thenReturn(3);

        lenient().when(sourceCoverage.getLine(anyInt())).thenAnswer(invocation -> {
            int lineNr = invocation.getArgument(0);
            ILine line = mock(ILine.class);

            if (lineNr == 1) {
                // Fully covered line
                setupLine(line, 2, 2, 1, 1);
            } else if (lineNr == 2) {
                // Partially covered line
                setupLine(line, 2, 1, 2, 1);
            } else if (lineNr == 3) {
                // Another partially covered line
                setupLine(line, 2, 1, 2, 1);
            }

            return line;
        });

        String result = mojo.formatMissingLines(sourceCoverage);

        assertEquals("partial: 2, 3", result);
    }
}