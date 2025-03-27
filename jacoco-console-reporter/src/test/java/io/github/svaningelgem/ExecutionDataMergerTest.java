package io.github.svaningelgem;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
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
        assertEquals("No classes should be processed", 0, merger.getProcessedClasses().size());
    }

    @Test
    public void testLoadExecutionDataWithNullFile() throws IOException {
        execFiles.add(null);
        ExecutionDataStore store = merger.loadExecutionData(execFiles);
        assertNotNull("Should handle null files gracefully", store);
        assertEquals("No classes should be processed", 0, merger.getProcessedClasses().size());
    }

    @Test
    public void testLoadExecutionDataWithNonExistentFile() throws IOException {
        execFiles.add(new File("nonexistent.exec"));
        ExecutionDataStore store = merger.loadExecutionData(execFiles);
        assertNotNull("Should handle non-existent files gracefully", store);
        assertEquals("No classes should be processed", 0, merger.getProcessedClasses().size());
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
    public void testMergeExecutionData() throws Exception {
        // This test is more complex and would require creating actual JaCoCo execution data
        // We'll use reflection to test the merging logic directly

        // Create a merged store with mocked data
        java.lang.reflect.Field mergedStoreField = ExecutionDataMerger.class.getDeclaredField("mergedStore");
        mergedStoreField.setAccessible(true);
        ExecutionDataStore mockStore = (ExecutionDataStore) mergedStoreField.get(merger);

        // Create two execution data instances with the same ID but different coverage
        long classId = 123456789L;
        ExecutionData data1 = new ExecutionData(classId, "TestClass", new boolean[]{true, false, false});
        ExecutionData data2 = new ExecutionData(classId, "TestClass", new boolean[]{false, true, false});

        // Add the first data to the store
        mockStore.put(data1);

        // Create an instance of SmartMergingVisitor and invoke it with the second data
        Class<?> visitorClass = Class.forName("io.github.svaningelgem.ExecutionDataMerger$SmartMergingVisitor");
        java.lang.reflect.Constructor<?> constructor = visitorClass.getDeclaredConstructor(ExecutionDataStore.class);
        constructor.setAccessible(true);
        Object visitor = constructor.newInstance(mockStore);

        java.lang.reflect.Method visitMethod = visitorClass.getDeclaredMethod("visitClassExecution", ExecutionData.class);
        visitMethod.setAccessible(true);

        // First visit should add the class to processedClasses
        visitMethod.invoke(visitor, data1);
        assertEquals(1, merger.getProcessedClasses().size());

        // Second visit should merge the probes
        visitMethod.invoke(visitor, data2);
        assertEquals(1, merger.getProcessedClasses().size());

        // Get the merged data and verify the probes were merged correctly
        ExecutionData mergedData = mockStore.get(classId);
        boolean[] mergedProbes = mergedData.getProbes();
        assertEquals(3, mergedProbes.length);
        assertTrue(mergedProbes[0]);
        assertTrue(mergedProbes[1]);
        assertFalse(mergedProbes[2]);
    }
}