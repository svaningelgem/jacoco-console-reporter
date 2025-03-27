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
     * Custom visitor that intelligently merges execution data at the probe level
     */
    private class MergingVisitor implements IExecutionDataVisitor {
        @Override
        public void visitClassExecution(ExecutionData data) {
            final Long classId = data.getId();
            final String className = data.getName();

            // Track this class
            processedClasses.put(classId, className);

            // Check if we already have data for this class
            ExecutionData existingData = mergedStore.get(classId);
            if (existingData != null) {
                // Merge the probe arrays (OR operation)
                // This is the key step - we combine coverage from different modules
                boolean[] existingProbes = existingData.getProbes();
                boolean[] newProbes = data.getProbes();

                if (existingProbes.length != newProbes.length) {
                    // This shouldn't normally happen, but we'll be defensive
                    // If lengths differ, create a new array with the max length
                    int maxLength = Math.max(existingProbes.length, newProbes.length);
                    boolean[] mergedProbes = new boolean[maxLength];

                    // Copy existing probes
                    System.arraycopy(existingProbes, 0, mergedProbes, 0, existingProbes.length);

                    // Merge with new probes - OR operation
                    for (int i = 0; i < newProbes.length; i++) {
                        if (i < mergedProbes.length) {
                            mergedProbes[i] |= newProbes[i];
                        }
                    }

                    // Create new execution data with merged probes
                    ExecutionData merged = new ExecutionData(classId, className, mergedProbes);
                    mergedStore.put(merged);
                } else {
                    // If lengths match, we can optimize by directly updating existing probes
                    for (int i = 0; i < newProbes.length; i++) {
                        existingProbes[i] |= newProbes[i];
                    }
                }
            } else {
                // First time seeing this class - just add it
                mergedStore.put(data);
            }
        }
    }
}