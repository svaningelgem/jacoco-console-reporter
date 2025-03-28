package io.github.svaningelgem;

import org.jacoco.core.data.ExecutionDataStore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JacocoConsoleReporterMojoLoadExecTest extends BaseTestClass {

    @Test
    public void testLoadExecutionDataWithNullExecFile() throws Exception {
        // Add a null element to collectedExecFilePaths
        JacocoConsoleReporterMojo.collectedExecFilePaths.add(null);

        // Call the method - it should handle null exec files
        Object result = loadExecutionData.invoke(mojo);
        assertNotNull(result);
        assertTrue(result instanceof ExecutionDataStore);

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
        Object result = loadExecutionData.invoke(mojo);
        assertNotNull(result);
        assertTrue(result instanceof ExecutionDataStore);
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
        loadExecFile.invoke(new ExecutionDataMerger(), mockExecFile, dataStore, sessionStore);
    }

    @Test
    public void testLoadExecFileWithNull() throws Exception {
        ExecutionDataStore dataStore = new ExecutionDataStore();
        org.jacoco.core.data.SessionInfoStore sessionStore = new org.jacoco.core.data.SessionInfoStore();

        // This should not throw an exception
        loadExecFile.invoke(new ExecutionDataMerger(), null, dataStore, sessionStore);
    }
}