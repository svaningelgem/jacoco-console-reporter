package io.github.svaningelgem;

import lombok.Getter;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.data.SessionInfoStore;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles merging execution data from multiple sources to prevent duplicated coverage counts
 */
public class ExecutionDataMerger {
    // Map to track processed class files by class name to avoid duplicates
    @Getter
    private final Map<String, Boolean> processedClasses = new HashMap<>();

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
            // Use a custom visitor to handle merging of execution data
            SmartMergingVisitor visitor = new SmartMergingVisitor(mergedStore);
            org.jacoco.core.data.ExecutionDataReader reader = new org.jacoco.core.data.ExecutionDataReader(in);
            reader.setExecutionDataVisitor(visitor);
            reader.setSessionInfoVisitor(sessionInfoStore);
            reader.read();
        }
    }

    /**
     * Custom visitor that intelligently merges execution data
     */
    private class SmartMergingVisitor implements org.jacoco.core.data.IExecutionDataVisitor {
        private final ExecutionDataStore store;

        SmartMergingVisitor(ExecutionDataStore store) {
            this.store = store;
        }

        @Override
        public void visitClassExecution(ExecutionData data) {
            final long classId = data.getId();
            final String className = getClassNameFromId(classId);

            // Only process each class once to avoid double counting
            if (!processedClasses.containsKey(className)) {
                processedClasses.put(className, true);
                store.put(data);
            } else {
                // If we've seen this class before, merge the execution data
                // instead of replacing it
                ExecutionData existing = store.get(classId);
                if (existing != null) {
                    boolean[] existingProbes = existing.getProbes();
                    boolean[] newProbes = data.getProbes();

                    // Merge probes: mark a probe as executed if it was executed in either execution
                    for (int i = 0; i < Math.min(existingProbes.length, newProbes.length); i++) {
                        existingProbes[i] |= newProbes[i];
                    }
                }
            }
        }

        /**
         * Try to determine class name from its ID.
         * This is a heuristic - JaCoCo uses CRC64 for IDs so we can't directly reverse it,
         * but we can use it to track unique classes.
         */
        private String getClassNameFromId(long id) {
            return "class_" + id;
        }
    }

    /**
     * Gets all execution data for analysis
     */
    public Collection<ExecutionData> getExecutionData() {
        return mergedStore.getContents();
    }

    /**
     * Gets all session info
     */
    public Collection<SessionInfo> getSessionInfos() {
        return sessionInfoStore.getInfos();
    }
}