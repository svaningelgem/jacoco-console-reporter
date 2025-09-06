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
    public void testBuildDirectoryTreeWithExcludedFiles() throws Exception {
        // Create test files in the build directory to match our exclusion patterns
        File targetDir = temporaryFolder.newFolder("target");
        File classesDir = new File(targetDir, "classes");

        // Configure project with our directories
        configureProjectForTesting(targetDir, classesDir, null);

        // Execute to set up targetDir and baseDir in mojo
        mojo.execute();

        // Create Java source files with package declarations for our test
        createFile(targetDir, "classes/com/example/ExcludedClass.java",
                "package com.example;\npublic class ExcludedClass {}");
        createFile(targetDir, "classes/com/example/IncludedClass.java",
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
        lenient().when(line.getInstructionCounter()).thenReturn(instrCounter);
        lenient().when(line.getBranchCounter()).thenReturn(branchCounter);
        lenient().when(line.getStatus()).thenReturn(computeStatus(instrCounter, branchCounter));
    }

    /**
     * Helper method to create a mock source file coverage
     */
    private @NotNull ISourceFileCoverage createMockSourceFileCoverage(String fileName) {
        ISourceFileCoverage sourceCoverage = mock(ISourceFileCoverage.class);
        lenient().when(sourceCoverage.getName()).thenReturn(fileName);

        ILine[] lines = new ILine[10];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = mock(ILine.class);
        }

        // Line 1: single fully covered line
        setupLine(lines[0], 1, 1, 0, 0);

        // Lines 2-4: 3 consecutive covered lines with fully covered branches
        for (int i = 1; i <= 3; i++) {
            setupLine(lines[i], 2, 1, 1, 1);
        }

        // Line 5: not covered with uncovered branch
        setupLine(lines[4], 0, 0, 1, 0);

        // Lines 6-9: partly covered lines
        for (int i = 5; i <= 8; i++) {
            setupLine(lines[i], 2, 1, 0, 0);
        }

        // Line 10: not covered line
        setupLine(lines[9], 1, 0, 0, 0);

        // Mock getLine
        lenient().when(sourceCoverage.getLine(anyInt())).thenAnswer(invocation -> {
            int lineNr = invocation.getArgument(0);
            if (lineNr < 1 || lineNr > 10) return null;
            return lines[lineNr - 1];
        });

        // Create mock counters for lines and branches
        ICounter lineCounter = mock(ICounter.class);
        lenient().when(lineCounter.getTotalCount()).thenReturn(10);
        lenient().when(lineCounter.getCoveredCount()).thenReturn(8);
        ICounter branchCounter = mock(ICounter.class);
        lenient().when(branchCounter.getTotalCount()).thenReturn(4);
        lenient().when(branchCounter.getCoveredCount()).thenReturn(3);
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

    @Test
    public void testCoverMissingInAddingDirTree() {
        mojo.showFiles = true;
        mojo.showMissingLines = true;

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

        int a = 1;

    }
}