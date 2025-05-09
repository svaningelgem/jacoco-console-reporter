package io.github.svaningelgem;

import org.jacoco.core.data.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles merging execution data from multiple sources to prevent duplicated coverage counts
 */
public class ExecutionDataMerger {
    final Set<Long> processedClasses = new HashSet<>();

    // Store to hold merged execution data
    final ExecutionDataStore mergedStore = new ExecutionDataStore();
    final SessionInfoStore sessionInfoStore = new SessionInfoStore();

    /**
     * Loads execution data from multiple files with deduplication
     *
     * @param execFiles Set of JaCoCo exec files to process
     * @return Merged execution data store
     * @throws IOException if there are issues reading the exec files
     */
    public @NotNull ExecutionDataStore loadExecutionData(@NotNull Set<File> execFiles) throws IOException {
        for (File execFile : execFiles) {
            loadExecFile(execFile, new MergingVisitor(), sessionInfoStore);
        }

        return mergedStore;
    }

    /**
     * Loads an individual JaCoCo execution data file
     */
    void loadExecFile(@Nullable File execFile, IExecutionDataVisitor executionDataStore, SessionInfoStore sessionInfoStore) throws IOException {
        if (execFile == null || !execFile.exists()) {
            return;
        }

        try (FileInputStream in = new FileInputStream(execFile)) {
            ExecutionDataReader reader = new ExecutionDataReader(in);
            reader.setExecutionDataVisitor(executionDataStore);
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
     * Merges execution data for testing purposes
     */
    public void mergeExecData(ExecutionData data) {
        if (data == null) {
            return;
        }

        processedClasses.add(data.getId());

        // Add to store (JaCoCo will handle the merging)
        mergedStore.put(data);
    }

    /**
     * Custom visitor that intelligently merges execution data
     */
    class MergingVisitor implements IExecutionDataVisitor {
        @Override
        public void visitClassExecution(ExecutionData data) {
            mergeExecData(data);
        }
    }
}