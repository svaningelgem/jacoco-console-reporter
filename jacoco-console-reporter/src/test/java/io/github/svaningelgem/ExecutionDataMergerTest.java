package io.github.svaningelgem;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.internal.data.CRC64;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ExecutionDataMergerTest extends BaseTestClass {
    private ExecutionDataMerger merger;
    private Set<File> execFiles;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        merger = new ExecutionDataMerger();
        execFiles = new HashSet<>();
    }

    @Test
    public void testLoadExecutionDataEmptySet() throws IOException {
        ExecutionDataStore store = merger.loadExecutionData(execFiles);
        assertNotNull("Should return a non-null store even with empty set", store);
        assertEquals("No classes should be processed", 0, merger.getUniqueClassCount());
    }

    @Test
    public void testLoadExecutionDataWithNullFile() throws IOException {
        execFiles.add(null);
        ExecutionDataStore store = merger.loadExecutionData(execFiles);
        assertNotNull("Should handle null files gracefully", store);
        assertEquals("No classes should be processed", 0, merger.getUniqueClassCount());
    }

    @Test
    public void testLoadExecutionDataWithNonExistentFile() throws IOException {
        execFiles.add(new File("nonexistent.exec"));
        ExecutionDataStore store = merger.loadExecutionData(execFiles);
        assertNotNull("Should handle non-existent files gracefully", store);
        assertEquals("No classes should be processed", 0, merger.getUniqueClassCount());
    }

    @Test
    public void testLoadExecutionDataWithInvalidFile() throws IOException {
        File invalidFile = temporaryFolder.newFile("invalid.exec");
        Files.write(invalidFile.toPath(), "not a valid JaCoCo exec file".getBytes());
        execFiles.add(invalidFile);

        try {
            merger.loadExecutionData(execFiles);
            fail("Should throw IOException for invalid file format");
        } catch (IOException e) {
            // Expected
        }
    }

    @Test
    public void testMergeExecutionData() {
        // Create test data
        String className = "com.example.TestClass";
        long classId = CRC64.classId(className.getBytes());

        // First execution: probes [true, false, false]
        ExecutionData data1 = new ExecutionData(classId, className, new boolean[] {true, false, false});

        // Second execution: probes [false, true, false]
        ExecutionData data2 = new ExecutionData(classId, className, new boolean[] {false, true, false});

        // Use the public method to merge data
        merger.mergeExecData(data1);
        merger.mergeExecData(data2);

        // Get merged results
        ExecutionDataStore mergedStore = merger.getMergedStore();
        ExecutionData mergedData = mergedStore.get(classId);

        // Verify the merged result
        assertNotNull("Merged data should exist", mergedData);
        assertEquals("Class ID should match", classId, mergedData.getId());
        assertEquals("Class name should match", className, mergedData.getName());

        boolean[] expectedProbes = new boolean[] {true, true, false};
        boolean[] actualProbes = mergedData.getProbes();

        assertEquals("Probe array length should match", expectedProbes.length, actualProbes.length);
        for (int i = 0; i < expectedProbes.length; i++) {
            assertEquals("Probe at index " + i + " should be merged correctly",
                    expectedProbes[i], actualProbes[i]);
        }
    }
}