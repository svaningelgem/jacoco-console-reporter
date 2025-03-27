package io.github.svaningelgem;

import org.jacoco.core.data.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles merging execution data from multiple sources to prevent duplicated coverage counts
 */
public class ExecutionDataMerger {
    // Map to track which classes we've processed (by class ID)
    private final Map<Long, String> processedClasses = new HashMap<>();

    // Store to hold merged execution data
    private final ExecutionDataStore mergedStore = new ExecutionDataStore();
    private final SessionInfoStore sessionInfoStore = new SessionInfoStore();

    /**
     * Loads execution data from multiple files with deduplication
     *
     * @param execFiles Set of JaCoCo exec files to process
     * @return Merged execution data store
     * @throws IOException if there are issues reading the exec files
     */
    public @NotNull ExecutionDataStore loadExecutionData(@NotNull Set<File> execFiles) throws IOException {
        for (File execFile : execFiles) {
            if (execFile == null || !execFile.exists()) {
                continue;
            }

            loadExecFile(execFile);
        }

        return mergedStore;
    }

    /**
     * Loads an individual JaCoCo execution data file
     */
    private void loadExecFile(@NotNull File execFile) throws IOException {
        try (FileInputStream in = new FileInputStream(execFile)) {
            ExecutionDataReader reader = new ExecutionDataReader(in);
            reader.setExecutionDataVisitor(new MergingVisitor());
            reader.setSessionInfoVisitor(sessionInfoStore);
            reader.read();
        }
    }

    /**
     * Get the number of unique classes processed
     */
    public int getUniqueClassCount() {
        return processedClasses.size();
    }

    /**
     * Get the merged execution data store (for testing)
     */
    public ExecutionDataStore getMergedStore() {
        return mergedStore;
    }

    /**
     * Merges execution data for testing purposes
     */
    public void mergeExecData(ExecutionData data) {
        if (data == null) {
            return;
        }

        // Track that we've seen this class
        processedClasses.put(data.getId(), data.getName());

        // Add to store (JaCoCo will handle the merging)
        mergedStore.put(data);
    }

    /**
     * Custom visitor that intelligently merges execution data
     */
    private class MergingVisitor implements IExecutionDataVisitor {
        @Override
        public void visitClassExecution(ExecutionData data) {
            final Long classId = data.getId();
            final String className = data.getName();

            // Track this class
            processedClasses.put(classId, className);

            // JaCoCo's ExecutionDataStore will automatically merge probe arrays
            // when you put() execution data with the same class ID
            mergedStore.put(data);
        }
    }
}