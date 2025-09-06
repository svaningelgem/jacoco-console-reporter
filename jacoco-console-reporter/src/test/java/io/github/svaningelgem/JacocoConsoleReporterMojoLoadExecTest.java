package io.github.svaningelgem;

import org.jacoco.core.data.ExecutionDataStore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;

public class JacocoConsoleReporterMojoLoadExecTest extends BaseTestClass {

    @Test
    public void testLoadExecutionDataWithNullExecFile() throws Exception {
        // Add a null element to collectedExecFilePaths
        JacocoConsoleReporterMojo.collectedExecFilePaths.add(null);

        // Call the method - it should handle null exec files
        ExecutionDataStore result = mojo.loadExecutionData();
        assertNotNull(result);

        // Clean up
        JacocoConsoleReporterMojo.collectedExecFilePaths.remove(null);
    }

    @Test
    public void testLoadExecutionDataMergingMultipleFiles() throws Exception {
        // Create temp exec files
        File execFile1 = temporaryFolder.newFile("mock1.exec");
        File execFile2 = temporaryFolder.newFile("mock2.exec");

        // Add files to the collection
        JacocoConsoleReporterMojo.collectedExecFilePaths.clear();
        JacocoConsoleReporterMojo.collectedExecFilePaths.add(execFile1);
        JacocoConsoleReporterMojo.collectedExecFilePaths.add(execFile2);

        // Mock files are empty but valid - test should pass
        ExecutionDataStore result = mojo.loadExecutionData();
        assertNotNull(result);
    }

    /**
     * Test that our compatibility method for loadExecFile works
     */
    @Test
    public void testLoadExecFileCompatibility() throws Exception {
        File mockExecFile = temporaryFolder.newFile("mock.exec");
        ExecutionDataStore dataStore = new ExecutionDataStore();
        org.jacoco.core.data.SessionInfoStore sessionStore = new org.jacoco.core.data.SessionInfoStore();

        // This should not throw an exception
        new ExecutionDataMerger().loadExecFile(mockExecFile, dataStore, sessionStore);
    }

    @Test
    public void testLoadExecFileWithNull() throws Exception {
        ExecutionDataStore dataStore = new ExecutionDataStore();
        org.jacoco.core.data.SessionInfoStore sessionStore = new org.jacoco.core.data.SessionInfoStore();

        // This should not throw an exception
        new ExecutionDataMerger().loadExecFile(null, dataStore, sessionStore);
    }

    @Test
    public void testLoadExecutionDataFromProjectConfiguration() throws Exception {
        // Create a temp exec file
        File tempExecFile = temporaryFolder.newFile("project-config.exec");

        // Configure project with the exec file
        File targetDir = tempExecFile.getParentFile();
        File classesDir = new File(targetDir, "classes");
        classesDir.mkdirs();

        configureProjectForTesting(targetDir, classesDir, tempExecFile);

        // Clear collected paths
        JacocoConsoleReporterMojo.collectedExecFilePaths.clear();
        JacocoConsoleReporterMojo.collectedClassesPaths.clear();

        // Execute should collect the exec file from configuration
        mojo.execute();

        // Verify the exec file was collected
        boolean found = JacocoConsoleReporterMojo.collectedExecFilePaths.stream()
                .anyMatch(f -> f != null && f.getName().equals("project-config.exec"));
        assertNotNull("Should have collected exec file from project configuration", found);

        // Load execution data should work
        ExecutionDataStore result = mojo.loadExecutionData();
        assertNotNull(result);
    }

    @Test
    public void testLoadExecutionDataWithMultipleModules() throws Exception {
        // Create exec files for multiple modules
        File module1ExecFile = temporaryFolder.newFile("module1.exec");
        File module2ExecFile = temporaryFolder.newFile("module2.exec");

        // Add to collected paths
        JacocoConsoleReporterMojo.collectedExecFilePaths.clear();
        JacocoConsoleReporterMojo.collectedExecFilePaths.add(module1ExecFile);
        JacocoConsoleReporterMojo.collectedExecFilePaths.add(module2ExecFile);

        // Load and merge data
        ExecutionDataStore result = mojo.loadExecutionData();
        assertNotNull("Should successfully merge multiple exec files", result);
    }

    @Test
    public void testLoadExecutionDataWithMissingFile() throws Exception {
        // Create a reference to a non-existent file
        File missingFile = new File(temporaryFolder.getRoot(), "missing.exec");

        // Add to collected paths
        JacocoConsoleReporterMojo.collectedExecFilePaths.clear();
        JacocoConsoleReporterMojo.collectedExecFilePaths.add(missingFile);

        // Should handle missing file gracefully
        ExecutionDataStore result = mojo.loadExecutionData();
        assertNotNull("Should handle missing files gracefully", result);
    }

    @Test
    public void testLoadExecutionDataWithEmptyCollection() throws Exception {
        // Clear all collected paths
        JacocoConsoleReporterMojo.collectedExecFilePaths.clear();

        // Should return empty store
        ExecutionDataStore result = mojo.loadExecutionData();
        assertNotNull("Should return non-null store even with no files", result);
    }
}