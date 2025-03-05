package io.github.svaningelgem;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;


public class JacocoConsoleReporterMojoTest extends BaseTestClass {
    @Test
    public void testExecuteWithNoExecFiles() throws Exception {
        assertNotNull("POM file not found", pom);
        assertTrue("POM file does not exist: " + pom.getAbsolutePath(), pom.exists());

        // Create a real project without JaCoCo plugin
        Model model = new Model();
        model.setGroupId("test.group");
        model.setArtifactId("test-artifact");
        model.setVersion("1.0.0");
        MavenProject project = new MavenProject(model);
        mojo.project = project;

        // Create a real MavenSession with this as the only project
        List<MavenProject> projects = new ArrayList<>();
        projects.add(project);
        mojo.mavenSession = createRealMavenSession(projects);

        // Set a non-existent jacoco.exec file
        File execFile = new File("target/nonexistent.exec");
        File classesDir = new File("target/classes");
        classesDir.mkdirs();

        mojo.jacocoExecFile = execFile;
        mojo.classesDirectory = classesDir;

        // Should log a warning and return without throwing an exception
        mojo.execute();
    }

    @Test
    public void testWithJacocoPlugin() throws Exception {
        // Set a non-existent file
        mojo.jacocoExecFile = new File("target/nonexistent.exec");
        mojo.classesDirectory = new File("target/classes");

        // Execute should detect the JaCoCo plugin
        mojo.execute();

        // No assertions needed - just verify it doesn't throw an exception
    }

    @Test
    public void testExecuteWithDeferredReportingFirstModule() throws Exception {
        assertNotNull("POM file not found", pom);
        assertTrue("POM file does not exist: " + pom.getAbsolutePath(), pom.exists());

        // Create the first project
        MavenProject project1 = createProjectWithJacocoPlugin(null);
        project1.setGroupId("test.group");
        project1.setArtifactId("module1");
        project1.setVersion("1.0.0");

        // Create the second project
        MavenProject project2 = createProjectWithJacocoPlugin(null);
        project2.setGroupId("test.group");
        project2.setArtifactId("module2");
        project2.setVersion("1.0.0");

        // Set current project to the first one
        mojo.project = project1;

        // Create a real MavenSession with multiple projects
        List<MavenProject> projects = Arrays.asList(project1, project2);
        mojo.mavenSession = createRealMavenSession(projects);

        // Set deferred reporting
        mojo.deferReporting = true;

        // Set a non-existent jacoco.exec file
        mojo.jacocoExecFile = new File("target/nonexistent.exec");
        mojo.classesDirectory = new File("target/classes");

        // Should log a message about deferring and return without exception
        mojo.execute();
    }

    @Test
    public void testExecuteWithDeferredReportingLastModule() throws Exception {
        assertNotNull("POM file not found", pom);
        assertTrue("POM file does not exist: " + pom.getAbsolutePath(), pom.exists());

        // Create the first project
        MavenProject project1 = createProjectWithJacocoPlugin(null);
        project1.setGroupId("test.group");
        project1.setArtifactId("module1");
        project1.setVersion("1.0.0");

        // Create the last project (our current one)
        MavenProject project2 = createProjectWithJacocoPlugin(null);
        project2.setGroupId("test.group");
        project2.setArtifactId("module2");
        project2.setVersion("1.0.0");
        mojo.project = project2;

        // Create a real MavenSession with multiple projects
        List<MavenProject> projects = Arrays.asList(project1, project2);
        mojo.mavenSession = createRealMavenSession(projects);

        // Set deferred reporting
        mojo.deferReporting = true;

        // Set a non-existent jacoco.exec file
        mojo.jacocoExecFile = new File("target/nonexistent.exec");
        mojo.classesDirectory = new File("target/classes");

        // Should execute since this is the last module
        mojo.execute();
    }

    @Test
    public void testExecuteWithShowFilesDisabled() throws Exception {
        if (!testProjectJacocoExec.exists() || !testProjectClasses.exists()) {
            // Skip test if test project files don't exist
            return;
        }

        assertNotNull("POM file not found", pom);
        assertTrue("POM file does not exist: " + pom.getAbsolutePath(), pom.exists());

        // Set showFiles to false
        mojo.showFiles = false;
        mojo.jacocoExecFile = testProjectJacocoExec;
        mojo.classesDirectory = testProjectClasses;

        // Should execute without throwing an exception
        mojo.execute();
    }

    @Test
    public void testScanModules() throws Exception {
        // Create a temporary directory structure with exec files
        File execFile1 = new File(temporaryFolder.getRoot(), "module1/target/jacoco.exec");
        File execFile2 = new File(temporaryFolder.getRoot(), "module2/target/jacoco.exec");

        execFile1.getParentFile().mkdirs();
        execFile2.getParentFile().mkdirs();

        // Create empty files
        Files.createFile(execFile1.toPath());
        Files.createFile(execFile2.toPath());

        // Configure mojo
        mojo.scanModules = true;
        mojo.jacocoExecFile = new File("nonexistent.exec");
        mojo.classesDirectory = testProjectClasses;

        assertEquals(0, JacocoConsoleReporterMojo.collectedExecFilePaths.size());

        // Execute and verify files were found
        mojo.execute();

        // Verify that both files were found
        assertEquals(3, JacocoConsoleReporterMojo.collectedExecFilePaths.size());
        assertTrue(JacocoConsoleReporterMojo.collectedExecFilePaths.contains(execFile1));
        assertTrue(JacocoConsoleReporterMojo.collectedExecFilePaths.contains(execFile2));
        assertTrue(JacocoConsoleReporterMojo.collectedExecFilePaths.contains(mojo.jacocoExecFile.getAbsoluteFile()));
    }

    @Test
    public void testCustomExecFilePattern() throws Exception {
        File baseDir = temporaryFolder.newFolder("custom-exec");
        File targetDir = new File(baseDir, "target");
        targetDir.mkdirs();

        // Create a custom exec file
        File customExec = new File(targetDir, "custom-coverage.exec");
        Files.createFile(customExec.toPath());

        // Configure the mojo
        // Create project with custom destFile
        MavenProject project = createProjectWithJacocoPlugin("${project.build.directory}/custom-coverage.exec");
        mojo.project = project;

        // Create a real MavenSession with this as the only project
        List<MavenProject> projects = new ArrayList<>();
        projects.add(project);
        mojo.mavenSession = createRealMavenSession(projects);

        // Configure the mojo
        mojo.baseDir = baseDir;
        mojo.scanModules = true;
        mojo.jacocoExecFile = new File("nonexistent.exec");
        mojo.classesDirectory = testProjectClasses;

        // Execute and verify custom file was found
        mojo.execute();

        // Verify the custom file was found
        boolean found = false;
        for (File file : JacocoConsoleReporterMojo.collectedExecFilePaths) {
            if (file.getAbsolutePath().equals(customExec.getAbsolutePath())) {
                found = true;
                break;
            }
        }
        assertTrue("Custom exec file was not found", found);
    }

    @Test
    public void testExecuteWithInvalidClassesDirectory() throws Exception {
        assertNotNull("POM file not found", pom);
        assertTrue("POM file does not exist: " + pom.getAbsolutePath(), pom.exists());

        // Create a temporary exec file
        File tempExecFile = temporaryFolder.newFile("temp.exec");

        // Use the temporary JaCoCo execution data but with an invalid classes directory
        File nonExistentDir = new File("nonexistent/classes");

        mojo.jacocoExecFile = testProjectJacocoExec;
        mojo.additionalExecFiles.add(tempExecFile);
        mojo.classesDirectory = nonExistentDir;

        mojo.execute();
    }

    @Test
    public void testExecute() throws Exception {
        if (!testProjectJacocoExec.exists() || !testProjectClasses.exists()) {
            // Skip test if test project files don't exist
            return;
        }

        assertNotNull("POM file not found", pom);
        assertTrue("POM file does not exist: " + pom.getAbsolutePath(), pom.exists());

        mojo.jacocoExecFile = testProjectJacocoExec;
        mojo.classesDirectory = testProjectClasses;

        // Should execute without throwing an exception
        mojo.execute();
    }

    @Test
    public void testTruncateMiddle() {
        // Test normal case
        String longString = "com.example.very.very.very.very.very.very.very.very.long.package.name";
        String truncated = Defaults.truncateMiddle(longString);
        assertEquals("com.example.very.very.ve..y.very.long.package.name", truncated);

        // Test string already shorter than max
        String shortString = "com.example";
        String notTruncated = Defaults.truncateMiddle(shortString);
        assertEquals(shortString, notTruncated);
    }

    @Test
    public void testShouldReport() throws Exception {
        // Create two projects for a multi-module build
        MavenProject project1 = createProjectWithJacocoPlugin(null);
        project1.setGroupId("test.group");
        project1.setArtifactId("module1");
        project1.setVersion("1.0.0");

        MavenProject project2 = createProjectWithJacocoPlugin(null);
        project2.setGroupId("test.group");
        project2.setArtifactId("module2");
        project2.setVersion("1.0.0");

        // Create a real MavenSession with both projects
        List<MavenProject> projects = Arrays.asList(project1, project2);
        MavenSession session = createRealMavenSession(projects);

        // Test with first project
        mojo.project = project1;
        mojo.mavenSession = session;
        mojo.deferReporting = true;

        // Access the private method using reflection
        java.lang.reflect.Method shouldReportMethod = JacocoConsoleReporterMojo.class.getDeclaredMethod("shouldReport");
        shouldReportMethod.setAccessible(true);

        boolean shouldReport = (boolean) shouldReportMethod.invoke(mojo);
        assertFalse("First module should not report when deferring", shouldReport);

        // Test with last project
        mojo.project = project2;
        shouldReport = (boolean) shouldReportMethod.invoke(mojo);
        assertTrue("Last module should report even when deferring", shouldReport);

        // Test without deferring
        mojo.project = project1;
        mojo.deferReporting = false;
        shouldReport = (boolean) shouldReportMethod.invoke(mojo);
        assertTrue("Module should report when not deferring", shouldReport);
    }
}