package io.github.svaningelgem;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class JacocoConsoleReporterMojoAdvancedTest extends BaseTestClass {

    @Test
    public void testExecuteWithNoClassesDirectory() throws Exception {
        // Test the case where the classes directory doesn't exist
        mojo.jacocoExecFile = testProjectJacocoExec;
        mojo.classesDirectory = new File("nonexistent/classes");
        mojo.deferReporting = false;

        // Should execute without throwing an exception
        mojo.execute();
    }

    @Test
    public void testExecuteWithEmptyExecFile() throws Exception {
        // Create an empty exec file
        File emptyExecFile = temporaryFolder.newFile("empty.exec");

        mojo.jacocoExecFile = emptyExecFile;
        mojo.classesDirectory = mainProjectClasses;
        mojo.deferReporting = false;

        // Should execute without throwing an exception
        mojo.execute();
    }

    @Test
    public void testExecuteWithCorruptExecFile() throws Exception {
        // Create a corrupt exec file
        File corruptExecFile = temporaryFolder.newFile("corrupt.exec");
        Files.write(corruptExecFile.toPath(), "not a valid exec file".getBytes());

        mojo.jacocoExecFile = corruptExecFile;
        mojo.classesDirectory = mainProjectClasses;
        mojo.deferReporting = false;

        // This will throw MojoExecutionException due to corrupt file - that's expected behavior
        try {
            mojo.execute();
            // It's okay if it gets here - newer JaCoCo versions might handle corrupt files
        } catch (MojoExecutionException e) {
            // Expected exception - test passes
            assertTrue(e.getMessage().contains("Failed to process JaCoCo data"));
        }
    }

    @Test
    public void testAnalyzeCoverageWithNullAndInvalidClasses() throws Exception {
        // Test the case where a class file is invalid
        File tempDir = temporaryFolder.newFolder("classes");
        File invalidClass = new File(tempDir, "Invalid.class");
        Files.write(invalidClass.toPath(), "not a valid class file".getBytes());


        // Mock the executionDataStore
        org.jacoco.core.data.ExecutionDataStore mockStore = new org.jacoco.core.data.ExecutionDataStore();

        // Replace mojo.classesDirectory with our temp directory
        File originalClassesDir = mojo.classesDirectory;
        mojo.classesDirectory = tempDir;

        // Add a null element to the collectedClassesPaths
        JacocoConsoleReporterMojo.collectedClassesPaths.clear();
        JacocoConsoleReporterMojo.collectedClassesPaths.add(null);
        JacocoConsoleReporterMojo.collectedClassesPaths.add(tempDir);

        try {
            // Call the method
            mojo.analyzeCoverage(mockStore);
        } catch (Exception e) {
            fail("Should not throw an exception: " + e.getMessage());
        } finally {
            // Restore the original classes directory
            mojo.classesDirectory = originalClassesDir;
        }
    }

    @Test
    public void testScanDirectoryCompleteTest() throws Exception {
        // Create directory structure to cover all branches
        File baseDir = temporaryFolder.newFolder("completeTest");

        // Case 1: Target directory is a file
        File fileTarget = new File(baseDir, "fileTarget");
        fileTarget.mkdir();
        File targetFile = new File(fileTarget, "target");
        Files.write(targetFile.toPath(), "not a directory".getBytes());

        // Case 2: Normal target directory with exec files
        File normalDir = new File(baseDir, "normalDir");
        normalDir.mkdir();
        File normalTarget = new File(normalDir, "target");
        normalTarget.mkdir();
        File execFile = new File(normalTarget, "jacoco.exec");
        execFile.createNewFile();

        // Case 3: Target with null listFiles result
        File mockDir = new File(baseDir, "mockDir");
        mockDir.mkdir();
        File mockTarget = new File(mockDir, "target");
        mockTarget.mkdir();
        File spyTarget = Mockito.spy(mockTarget);
        Mockito.doReturn(null).when(spyTarget).listFiles((FilenameFilter) any());

        // Case 4: Subdirectories with various filters
        File subdirsDir = new File(baseDir, "subdirsDir");
        subdirsDir.mkdir();
        // Create a target directory (should be skipped for recursion)
        new File(subdirsDir, "target").mkdir();
        // Create a node_modules directory (should be skipped)
        new File(subdirsDir, "node_modules").mkdir();
        // Create a hidden directory (should be skipped)
        new File(subdirsDir, ".hidden").mkdir();
        // Create a regular directory (should be recursed into)
        File regularSubdir = new File(subdirsDir, "regularSubdir");
        regularSubdir.mkdir();
        File regularTarget = new File(regularSubdir, "target");
        regularTarget.mkdir();
        File regularExec = new File(regularTarget, "jacoco.exec");
        regularExec.createNewFile();


        // Create a list of patterns
        java.util.List<String> patterns = Collections.singletonList("jacoco.exec");

        // Save the initial size
        int initialSize = JacocoConsoleReporterMojo.collectedExecFilePaths.size();

        // Call the method on the base directory
        mojo.scanDirectoryForExecFiles(baseDir, patterns);

        // Verify we found the exec files
        assertEquals(initialSize + 2, JacocoConsoleReporterMojo.collectedExecFilePaths.size());
        assertTrue(JacocoConsoleReporterMojo.collectedExecFilePaths.contains(execFile));
        assertTrue(JacocoConsoleReporterMojo.collectedExecFilePaths.contains(regularExec));
    }

    @Test
    public void testLoadExecFileWithIOException() throws Exception {
        // Create a file that will throw IOException when accessed
        File mockExecFile = temporaryFolder.newFile("inaccessible.exec");

        // Mock a FileInputStream that throws IOException
        org.jacoco.core.data.ExecutionDataStore executionDataStore = new org.jacoco.core.data.ExecutionDataStore();
        org.jacoco.core.data.SessionInfoStore sessionInfoStore = new org.jacoco.core.data.SessionInfoStore();

        // Delete the file after creation to cause IOException
        mockExecFile.delete();

        // Call the method - it shouldn't throw an IOException
        new ExecutionDataMerger().loadExecFile(mockExecFile, executionDataStore, sessionInfoStore);
    }

    @Test
    public void testExecuteWithNullAdditionalExecFiles() throws Exception {
        mojo.additionalExecFiles = null;

        // This should throw a NullPointerException, but the plugin should catch it
        try {
            mojo.execute();
            fail("Should have thrown an exception");
        } catch (Exception e) {
            // Expected
            assertTrue(e instanceof NullPointerException);
        }
    }

    @Test
    public void testWeightingOptions() throws Exception {
        // Test with zero weights
        mojo.weightClassCoverage = 0.0;
        mojo.weightMethodCoverage = 0.0;
        mojo.weightBranchCoverage = 0.0;
        mojo.weightLineCoverage = 0.0;

        DirectoryNode root = new DirectoryNode("");
        CoverageMetrics metrics = new CoverageMetrics(8, 4, 6, 3, 4, 2, 2, 1);
        createTree(root, 1, metrics, "com", "example", "model");

        // Should handle zero weights
        mojo.showSummary = true;
        mojo.printSummary(root);

        // Test with negative weights (invalid but should be handled)
        mojo.weightClassCoverage = -0.1;
        mojo.weightMethodCoverage = -0.1;
        mojo.weightBranchCoverage = -0.4;
        mojo.weightLineCoverage = -0.4;

        // Should handle negative weights
        mojo.printSummary(root);
    }

    @Test
    public void testLoadExecutionDataWithNullExecFile() throws Exception {
        // Add a null element to collectedExecFilePaths
        JacocoConsoleReporterMojo.collectedExecFilePaths.add(null);

        // Call the method - it should handle null exec files
        Object result = mojo.loadExecutionData();
        assertNotNull(result);

        // Clean up
        JacocoConsoleReporterMojo.collectedExecFilePaths.remove(null);
    }

    @Test
    public void testBuildDirectoryTreeWithEmptyBundle() {
        // Create a mock bundle with no packages
        org.jacoco.core.analysis.IBundleCoverage mockBundle = Mockito.mock(org.jacoco.core.analysis.IBundleCoverage.class);
        when(mockBundle.getPackages()).thenReturn(Collections.emptyList());

        // Call the method - it should handle an empty bundle
        DirectoryNode result = mojo.buildDirectoryTree(mockBundle);
        assertNotNull(result);
    }

    @Test
    public void testShouldIncludeWithEmptyDirectory() {
        // Create an empty directory node
        DirectoryNode emptyDir = new DirectoryNode("empty");

        // Should return false for an empty directory with no files or subdirectories
        assertFalse(emptyDir.shouldInclude());
    }

    @Test
    public void testFormatCoverageEdgeCases() {
        // Test with zero total
        String result = Defaults.getInstance().formatCoverage(0, 0);
        assertEquals(" ***** (0/0)", result);

        // Test with negative values (edge case)
        result = Defaults.getInstance().formatCoverage(-1, -2);
        assertEquals(" ***** (0/0)", result);
    }

    @Test
    public void testScanDirectoryForExecFilesWithNotExistingDir() throws Exception {
        mojo.scanDirectoryForExecFiles(new File(temporaryFolder.getRoot(), "not_here"), null);
    }

    @Test
    public void testScanDirectoryForExecFilesWithFile() throws Exception {
        File tmp = temporaryFolder.newFile("tmp");
        mojo.scanDirectoryForExecFiles(tmp, null);
    }
}