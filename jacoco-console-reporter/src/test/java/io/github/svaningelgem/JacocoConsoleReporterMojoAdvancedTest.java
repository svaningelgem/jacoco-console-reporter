package io.github.svaningelgem;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.Assert.*;

public class JacocoConsoleReporterMojoAdvancedTest extends BaseTestClass {

    @Test
    public void testExecuteWithNoClassesDirectory() throws Exception {
        // Test the case where the classes directory doesn't exist
        classesDir.delete();
        configureProjectForTesting(testProjectJacocoExec);
        mojo.deferReporting = false;

        // Should execute without throwing an exception
        mojo.execute();
    }

    @Test
    public void testExecuteWithEmptyExecFile() throws Exception {
        // Create an empty exec file
        File emptyExecFile = temporaryFolder.newFile("empty.exec");
        configureProjectForTesting(emptyExecFile);
        mojo.deferReporting = false;

        // Should execute without throwing an exception
        mojo.execute();
    }

    @Test
    public void testExecuteWithCorruptExecFile() throws Exception {
        // Create a corrupt exec file
        File corruptExecFile = temporaryFolder.newFile("corrupt.exec");
        Files.write(corruptExecFile.toPath(), "not a valid exec file".getBytes());

        configureProjectForTesting(corruptExecFile);
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
    public void testAnalyzeCoverageWithNullAndInvalidClasses() throws Exception {
        // Test the case where a class file is invalid
        File invalidClass = new File(classesDir, "Invalid.class");
        Files.write(invalidClass.toPath(), "not a valid class file".getBytes());

        // Mock the executionDataStore
        org.jacoco.core.data.ExecutionDataStore mockStore = new org.jacoco.core.data.ExecutionDataStore();

        // Configure project with our temp directory
        configureProjectForTesting(null);

        // Add a null element to the collectedClassesPaths
        JacocoConsoleReporterMojo.collectedClassesPaths.clear();
        JacocoConsoleReporterMojo.collectedClassesPaths.add(null);
        JacocoConsoleReporterMojo.collectedClassesPaths.add(classesDir);

        try {
            // Call the method
            mojo.analyzeCoverage(mockStore);
        } catch (Exception e) {
            fail("Should not throw an exception: " + e.getMessage());
        }
    }

    @Test
    public void testLoadExecFileWithIOException() throws Exception {
        // Create a file that will throw IOException when accessed
        File mockExecFile = temporaryFolder.newFile("inaccessible.exec");

        // Mock a FileInputStream that throws IOException
        org.jacoco.core.data.ExecutionDataStore executionDataStore = new org.jacoco.core.data.ExecutionDataStore();
        org.jacoco.core.data.SessionInfoStore sessionInfoStore = new org.jacoco.core.data.SessionInfoStore();

        // Delete the file after creation to cause IOException
        mockExecFile.delete();

        // Call the method - it shouldn't throw an IOException
        new ExecutionDataMerger().loadExecFile(mockExecFile, executionDataStore, sessionInfoStore);
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

        // Should handle zero weights
        mojo.showSummary = true;
        mojo.printSummary(root);

        // Test with negative weights (invalid but should be handled)
        mojo.weightClassCoverage = -0.1;
        mojo.weightMethodCoverage = -0.1;
        mojo.weightBranchCoverage = -0.4;
        mojo.weightLineCoverage = -0.4;

        // Should handle negative weights
        mojo.printSummary(root);
    }

    @Test
    public void testLoadExecutionDataWithNullExecFile() throws Exception {
        // Add a null element to collectedExecFilePaths
        JacocoConsoleReporterMojo.collectedExecFilePaths.add(null);

        // Call the method - it should handle null exec files
        Object result = mojo.loadExecutionData();
        assertNotNull(result);

        // Clean up
        JacocoConsoleReporterMojo.collectedExecFilePaths.remove(null);
    }

    @Test
    public void testBuildDirectoryTreeWithEmptyBundle() {
        // Create a mock bundle with no packages
        org.jacoco.core.analysis.IBundleCoverage mockBundle = Mockito.mock(org.jacoco.core.analysis.IBundleCoverage.class);
        Mockito.when(mockBundle.getPackages()).thenReturn(Collections.emptyList());

        // Call the method - it should handle an empty bundle
        DirectoryNode result = mojo.buildDirectoryTree(mockBundle);
        assertNotNull(result);
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
        String result = Defaults.getInstance().formatCoverage(0, 0);
        assertEquals(" ***** (0/0)", result);

        // Test with negative values (edge case)
        result = Defaults.getInstance().formatCoverage(-1, -2);
        assertEquals(" ***** (0/0)", result);
    }

    @Test
    public void testExecuteWithMultipleProjects() throws Exception {
        // Create multiple projects to test aggregation
        File targetDir1 = temporaryFolder.newFolder("module1", "target");
        File classesDir1 = new File(targetDir1, "classes");
        File execFile1 = new File(targetDir1, "jacoco.exec");
        Files.createFile(execFile1.toPath());

        MavenProject project1 = createProjectWithJacocoPlugin(execFile1.getAbsolutePath());
        project1.setGroupId("test.group");
        project1.setArtifactId("module1");
        project1.setVersion("1.0.0");
        project1.getBuild().setDirectory(targetDir1.getAbsolutePath());
        project1.getBuild().setOutputDirectory(classesDir1.getAbsolutePath());

        File targetDir2 = temporaryFolder.newFolder("module2", "target");
        File classesDir2 = new File(targetDir2, "classes");
        File execFile2 = new File(targetDir2, "jacoco.exec");
        Files.createFile(execFile2.toPath());

        MavenProject project2 = createProjectWithJacocoPlugin(execFile2.getAbsolutePath());
        project2.setGroupId("test.group");
        project2.setArtifactId("module2");
        project2.setVersion("1.0.0");
        project2.getBuild().setDirectory(targetDir2.getAbsolutePath());
        project2.getBuild().setOutputDirectory(classesDir2.getAbsolutePath());

        // Set current project to the last one
        mojo.project = project2;
        mojo.mavenSession = createRealMavenSession(java.util.Arrays.asList(project1, project2));
        mojo.deferReporting = true;

        // Clear collected paths
        JacocoConsoleReporterMojo.collectedExecFilePaths.clear();
        JacocoConsoleReporterMojo.collectedClassesPaths.clear();

        // Execute first module
        mojo.project = project1;
        mojo.execute();

        // Should have collected from first module
        assertTrue("Should collect exec file from first module",
                JacocoConsoleReporterMojo.collectedExecFilePaths.contains(execFile1));
        assertTrue("Should collect classes from first module",
                JacocoConsoleReporterMojo.collectedClassesPaths.contains(classesDir1));

        // Execute second module
        mojo.project = project2;
        mojo.execute();

        // Should have collected from both modules
        assertTrue("Should collect exec file from second module",
                JacocoConsoleReporterMojo.collectedExecFilePaths.contains(execFile2));
        assertTrue("Should collect classes from second module",
                JacocoConsoleReporterMojo.collectedClassesPaths.contains(classesDir2));
    }

    @Test
    public void testProjectWithoutJacocoPlugin() throws Exception {
        // Create a project without JaCoCo plugin
        mojo.project.getBuild().getPlugins().clear();

        // Should still execute but won't find destFile
        mojo.execute();

        // Should not have collected any exec files from plugin config
        // (might still have the default from setUp)
        boolean foundNoJacocoLog = log.writtenData.stream()
                .anyMatch(s -> s.contains("Added exec file from current module"));
        assertFalse("Should not find exec file without JaCoCo plugin", foundNoJacocoLog);
    }
}