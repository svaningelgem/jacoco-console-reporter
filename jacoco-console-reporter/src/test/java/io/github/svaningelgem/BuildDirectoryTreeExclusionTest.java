package io.github.svaningelgem;

import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
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
        when(packageCoverage.getName()).thenReturn(packageName);
        return packageCoverage;
    }

    /**
     * Helper method to create a mock source file coverage
     */
    private @NotNull ISourceFileCoverage createMockSourceFileCoverage(String fileName) {
        ISourceFileCoverage sourceCoverage = mock(ISourceFileCoverage.class);
        when(sourceCoverage.getName()).thenReturn(fileName);

        // Create mock counters for lines and branches
        ICounter lineCounter = mock(ICounter.class);
        when(lineCounter.getTotalCount()).thenReturn(10);
        when(lineCounter.getCoveredCount()).thenReturn(8);

        ICounter branchCounter = mock(ICounter.class);
        when(branchCounter.getTotalCount()).thenReturn(4);
        when(branchCounter.getCoveredCount()).thenReturn(3);

        when(sourceCoverage.getLineCounter()).thenReturn(lineCounter);
        when(sourceCoverage.getBranchCounter()).thenReturn(branchCounter);

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
}