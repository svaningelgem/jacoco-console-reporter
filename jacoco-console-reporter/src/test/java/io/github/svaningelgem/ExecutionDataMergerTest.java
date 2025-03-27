package io.github.svaningelgem;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    public void testMergeExecutionDataWithEqualProbeLengths() throws Exception {
        // Get the private mergedStore field for verification
        Field mergedStoreField = ExecutionDataMerger.class.getDeclaredField("mergedStore");
        mergedStoreField.setAccessible(true);
        ExecutionDataStore store = (ExecutionDataStore) mergedStoreField.get(merger);

        // Create MergingVisitor instance using reflection
        Class<?> visitorClass = Class.forName("io.github.svaningelgem.ExecutionDataMerger$MergingVisitor");
        Object visitor = visitorClass.getDeclaredConstructors()[0].newInstance(merger);

        // Create test execution data
        long classId = 123456789L;
        String className = "com.example.TestClass";

        // First execution: probes [true, false, false]
        ExecutionData data1 = new ExecutionData(classId, className, new boolean[] {true, false, false});

        // Second execution: probes [false, true, false]
        ExecutionData data2 = new ExecutionData(classId, className, new boolean[] {false, true, false});

        // Expected merged result: [true, true, false]

        // Invoke visitClassExecution method
        Method visitMethod = visitorClass.getMethod("visitClassExecution", ExecutionData.class);
        visitMethod.invoke(visitor, data1); // First visit puts data1 in store
        visitMethod.invoke(visitor, data2); // Second visit should merge data2 with data1

        // Verify the merged result
        ExecutionData mergedData = store.get(classId);
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

    @Test
    public void testMergeExecutionDataWithDifferentProbeLengths() throws Exception {
        // Get the private mergedStore field for verification
        Field mergedStoreField = ExecutionDataMerger.class.getDeclaredField("mergedStore");
        mergedStoreField.setAccessible(true);
        ExecutionDataStore store = (ExecutionDataStore) mergedStoreField.get(merger);

        // Create MergingVisitor instance using reflection
        Class<?> visitorClass = Class.forName("io.github.svaningelgem.ExecutionDataMerger$MergingVisitor");
        Object visitor = visitorClass.getDeclaredConstructors()[0].newInstance(merger);

        // Create test execution data
        long classId = 123456789L;
        String className = "com.example.TestClass";

        // First execution: probes [true, false]
        ExecutionData data1 = new ExecutionData(classId, className, new boolean[] {true, false});

        // Second execution: probes [false, true, true]
        ExecutionData data2 = new ExecutionData(classId, className, new boolean[] {false, true, true});

        // Expected merged result: [true, true, true]

        // Invoke visitClassExecution method
        Method visitMethod = visitorClass.getMethod("visitClassExecution", ExecutionData.class);
        visitMethod.invoke(visitor, data1); // First visit puts data1 in store
        visitMethod.invoke(visitor, data2); // Second visit should merge data2 with data1

        // Verify the merged result
        ExecutionData mergedData = store.get(classId);
        assertNotNull("Merged data should exist", mergedData);
        assertEquals("Class ID should match", classId, mergedData.getId());
        assertEquals("Class name should match", className, mergedData.getName());

        boolean[] expectedProbes = new boolean[] {true, true, true};
        boolean[] actualProbes = mergedData.getProbes();

        assertEquals("Probe array length should match largest input", 3, actualProbes.length);
        for (int i = 0; i < expectedProbes.length; i++) {
            assertEquals("Probe at index " + i + " should be merged correctly",
                    expectedProbes[i], actualProbes[i]);
        }
    }

    @Test
    public void testMergeExecutionDataFirstIsLarger() throws Exception {
        // Get the private mergedStore field for verification
        Field mergedStoreField = ExecutionDataMerger.class.getDeclaredField("mergedStore");
        mergedStoreField.setAccessible(true);
        ExecutionDataStore store = (ExecutionDataStore) mergedStoreField.get(merger);

        // Create MergingVisitor instance using reflection
        Class<?> visitorClass = Class.forName("io.github.svaningelgem.ExecutionDataMerger$MergingVisitor");
        Object visitor = visitorClass.getDeclaredConstructors()[0].newInstance(merger);

        // Create test execution data
        long classId = 123456789L;
        String className = "com.example.TestClass";

        // First execution: probes [true, false, true]
        ExecutionData data1 = new ExecutionData(classId, className, new boolean[] {true, false, true});

        // Second execution: probes [false, true]
        ExecutionData data2 = new ExecutionData(classId, className, new boolean[] {false, true});

        // Expected merged result: [true, true, true]

        // Invoke visitClassExecution method
        Method visitMethod = visitorClass.getMethod("visitClassExecution", ExecutionData.class);
        visitMethod.invoke(visitor, data1); // First visit puts data1 in store
        visitMethod.invoke(visitor, data2); // Second visit should merge data2 with data1

        // Verify the merged result
        ExecutionData mergedData = store.get(classId);
        assertNotNull("Merged data should exist", mergedData);
        assertEquals("Class ID should match", classId, mergedData.getId());
        assertEquals("Class name should match", className, mergedData.getName());

        boolean[] expectedProbes = new boolean[] {true, true, true};
        boolean[] actualProbes = mergedData.getProbes();

        assertEquals("Probe array length should match largest input", 3, actualProbes.length);
        for (int i = 0; i < expectedProbes.length; i++) {
            assertEquals("Probe at index " + i + " should be merged correctly",
                    expectedProbes[i], actualProbes[i]);
        }
    }
}