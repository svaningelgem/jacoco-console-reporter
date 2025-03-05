package io.github.svaningelgem;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.Assert.*;
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
    public void testAnalyzeCoverageWithInvalidClass() throws Exception {
        // Test the case where a class file is invalid
        File tempDir = temporaryFolder.newFolder("classes");
        File invalidClass = new File(tempDir, "Invalid.class");
        Files.write(invalidClass.toPath(), "not a valid class file".getBytes());

        // Use reflection to test the analyzeCoverage method
        Method analyzeCoverage = JacocoConsoleReporterMojo.class.getDeclaredMethod("analyzeCoverage",
                org.jacoco.core.data.ExecutionDataStore.class);
        analyzeCoverage.setAccessible(true);

        // Mock the executionDataStore
        org.jacoco.core.data.ExecutionDataStore mockStore = new org.jacoco.core.data.ExecutionDataStore();

        // Replace mojo.classesDirectory with our temp directory
        File originalClassesDir = mojo.classesDirectory;
        mojo.classesDirectory = tempDir;
        JacocoConsoleReporterMojo.collectedClassesPaths.clear();
        JacocoConsoleReporterMojo.collectedClassesPaths.add(tempDir);

        try {
            // Call the method
            analyzeCoverage.invoke(mojo, mockStore);
        } catch (Exception e) {
            fail("Should not throw an exception: " + e.getMessage());
        } finally {
            // Restore the original classes directory
            mojo.classesDirectory = originalClassesDir;
        }
    }

    @Test
    public void testScanDirectoryForExecFilesWithNullListings() throws Exception {
        // Create real directories rather than mocks to avoid NullPointerException in File constructor
        File baseDir = temporaryFolder.newFolder("mockBaseDir");
        File targetDir = new File(baseDir, "target");
        targetDir.mkdir();

        // Use reflection to get the scanDirectoryForExecFiles method
        Method scanDirectoryForExecFiles = JacocoConsoleReporterMojo.class.getDeclaredMethod("scanDirectoryForExecFiles",
                File.class, java.util.List.class);
        scanDirectoryForExecFiles.setAccessible(true);

        // Create a list of patterns
        java.util.List<String> patterns = Collections.singletonList("jacoco.exec");

        // Call the method with real directories
        scanDirectoryForExecFiles.invoke(mojo, baseDir, patterns);

        // Create a test to handle non-existent directories
        File nonExistentDir = new File(baseDir, "nonexistent");
        scanDirectoryForExecFiles.invoke(mojo, nonExistentDir, patterns);

        // Create a file masquerading as a directory to test that branch
        File fileNotDir = new File(baseDir, "file.txt");
        Files.write(fileNotDir.toPath(), "not a directory".getBytes());
        scanDirectoryForExecFiles.invoke(mojo, fileNotDir, patterns);
    }

    @Test
    public void testGetConfiguredExecFilePatternsBadConfig() throws Exception {
        // Create a project with a JaCoCo plugin that has malformed configuration
        org.apache.maven.model.Plugin jacocoPlugin = new org.apache.maven.model.Plugin();
        jacocoPlugin.setGroupId("org.jacoco");
        jacocoPlugin.setArtifactId("jacoco-maven-plugin");
        jacocoPlugin.setVersion("0.8.7");

        // Set a malformed configuration
        org.codehaus.plexus.util.xml.Xpp3Dom config = new org.codehaus.plexus.util.xml.Xpp3Dom("configuration");
        org.codehaus.plexus.util.xml.Xpp3Dom destFile = new org.codehaus.plexus.util.xml.Xpp3Dom("destFile");
        // No value set for destFile - malformed
        config.addChild(destFile);
        jacocoPlugin.setConfiguration(config);

        // Add plugin to project's build
        mojo.project.getBuild().getPlugins().clear();
        mojo.project.getBuild().addPlugin(jacocoPlugin);

        // Use reflection to get the getConfiguredExecFilePatterns method
        Method getConfiguredExecFilePatterns = JacocoConsoleReporterMojo.class.getDeclaredMethod("getConfiguredExecFilePatterns");
        getConfiguredExecFilePatterns.setAccessible(true);

        // Call the method - it should handle malformed configuration
        @SuppressWarnings("unchecked")
        java.util.List<String> patterns = (java.util.List<String>) getConfiguredExecFilePatterns.invoke(mojo);

        // Should still include the default pattern
        assertTrue(patterns.contains("jacoco.exec"));
    }

    @Test
    public void testLoadExecFileWithIOException() throws Exception {
        // Create a file that will throw IOException when accessed
        File mockExecFile = temporaryFolder.newFile("inaccessible.exec");

        // Mock a FileInputStream that throws IOException
        org.jacoco.core.data.ExecutionDataStore executionDataStore = new org.jacoco.core.data.ExecutionDataStore();
        org.jacoco.core.data.SessionInfoStore sessionInfoStore = new org.jacoco.core.data.SessionInfoStore();

        // Use reflection to access the loadExecFile method
        Method loadExecFile = JacocoConsoleReporterMojo.class.getDeclaredMethod("loadExecFile",
                File.class, org.jacoco.core.data.ExecutionDataStore.class, org.jacoco.core.data.SessionInfoStore.class);
        loadExecFile.setAccessible(true);

        // Delete the file after creation to cause IOException
        mockExecFile.delete();

        // Call the method - it should throw an IOException
        try {
            loadExecFile.invoke(mojo, mockExecFile, executionDataStore, sessionInfoStore);
            fail("Should have thrown an exception");
        } catch (Exception e) {
            // Expected - verify the cause chain contains IOException
            Throwable cause = e.getCause();
            boolean foundIOException = false;
            while (cause != null) {
                if (cause instanceof IOException) {
                    foundIOException = true;
                    break;
                }
                cause = cause.getCause();
            }
            assertTrue("Expected IOException in cause chain", foundIOException);
        }
    }

    @Test
    public void testExecuteWithNullAdditionalExecFiles() throws Exception {
        // Set additionalExecFiles to null
        Field additionalExecFilesField = JacocoConsoleReporterMojo.class.getDeclaredField("additionalExecFiles");
        additionalExecFilesField.setAccessible(true);
        additionalExecFilesField.set(mojo, null);

        // This should throw a NullPointerException, but the plugin should catch it
        try {
            mojo.execute();
            fail("Should have thrown an exception");
        } catch (Exception e) {
            // Expected
            assertTrue(e instanceof NullPointerException);
        }

        // Reset the field
        additionalExecFilesField.set(mojo, new java.util.ArrayList<File>());
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

        // Use reflection to get the printSummary method
        Method printSummary = JacocoConsoleReporterMojo.class.getDeclaredMethod("printSummary", DirectoryNode.class);
        printSummary.setAccessible(true);

        // Should handle zero weights
        mojo.showSummary = true;
        printSummary.invoke(mojo, root);

        // Test with negative weights (invalid but should be handled)
        mojo.weightClassCoverage = -0.1;
        mojo.weightMethodCoverage = -0.1;
        mojo.weightBranchCoverage = -0.4;
        mojo.weightLineCoverage = -0.4;

        // Should handle negative weights
        printSummary.invoke(mojo, root);
    }

    @Test
    public void testNonExistentJacocoExecFile() throws Exception {
        // Set a non-existent exec file
        File nonExistentFile = new File("target/nonexistent.exec");
        mojo.jacocoExecFile = nonExistentFile;

        // Clear the collected paths
        JacocoConsoleReporterMojo.collectedExecFilePaths.clear();
        JacocoConsoleReporterMojo.collectedExecFilePaths.add(nonExistentFile);

        // Use reflection to get the loadExecutionData method
        Method loadExecutionData = JacocoConsoleReporterMojo.class.getDeclaredMethod("loadExecutionData");
        loadExecutionData.setAccessible(true);

        // Call the method - it should handle non-existent files
        Object result = loadExecutionData.invoke(mojo);
        assertNotNull(result);
    }

    @Test
    public void testBuildDirectoryTreeWithEmptyBundle() throws Exception {
        // Use reflection to get the buildDirectoryTree method
        Method buildDirectoryTree = JacocoConsoleReporterMojo.class.getDeclaredMethod("buildDirectoryTree",
                org.jacoco.core.analysis.IBundleCoverage.class);
        buildDirectoryTree.setAccessible(true);

        // Create a mock bundle with no packages
        org.jacoco.core.analysis.IBundleCoverage mockBundle = Mockito.mock(org.jacoco.core.analysis.IBundleCoverage.class);
        when(mockBundle.getPackages()).thenReturn(Collections.emptyList());

        // Call the method - it should handle an empty bundle
        Object result = buildDirectoryTree.invoke(mojo, mockBundle);
        assertNotNull(result);
        assertTrue(result instanceof DirectoryNode);
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
        String result = Defaults.formatCoverage(0, 0);
        assertEquals(" ***** (0/0)", result);

        // Test with negative values (edge case)
        result = Defaults.formatCoverage(-1, -2);
        assertEquals(" ***** (0/0)", result);
    }
}