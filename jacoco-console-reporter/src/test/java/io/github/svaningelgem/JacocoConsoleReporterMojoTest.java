package io.github.svaningelgem;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JacocoConsoleReporterMojoTest {

    @Rule
    public final MojoRule rule = new MojoRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final File testProjectDir = new File("../test-project");
    private final File testProjectJacocoExec = new File(testProjectDir, "target/jacoco.exec");
    private final File testProjectClasses = new File(testProjectDir, "target/classes");
    private final File pom = new File(getBasedir(), "src/test/resources/unit/pom.xml");

    /**
     * Creates a real MavenProject with JaCoCo plugin configuration
     */
    private MavenProject createProjectWithJacocoPlugin(String destFile) {
        Model model = new Model();
        model.setGroupId("test.group");
        model.setArtifactId("test-artifact");
        model.setVersion("1.0.0");

        Build build = new Build();
        model.setBuild(build);

        Plugin plugin = new Plugin();
        plugin.setGroupId("org.jacoco");
        plugin.setArtifactId("jacoco-maven-plugin");
        plugin.setVersion("0.8.12");

        if (destFile != null) {
            Xpp3Dom configuration = new Xpp3Dom("configuration");
            Xpp3Dom destFileNode = new Xpp3Dom("destFile");
            destFileNode.setValue(destFile);
            configuration.addChild(destFileNode);
            plugin.setConfiguration(configuration);
        }

        build.addPlugin(plugin);

        return new MavenProject(model);
    }

    /**
     * Creates a mock MavenSession with multiple projects
     */
    private MavenSession createMavenSession(MavenProject currentProject, boolean isLast) {
        List<MavenProject> projects = new ArrayList<>();

        // Create first project
        MavenProject project1 = createProjectWithJacocoPlugin(null);
        project1.setGroupId("test.group");
        project1.setArtifactId("module1");
        project1.setVersion("1.0.0");
        projects.add(project1);

        // If we need more than one project
        if (!isLast || currentProject != project1) {
            MavenProject project2 = createProjectWithJacocoPlugin(null);
            project2.setGroupId("test.group");
            project2.setArtifactId("module2");
            project2.setVersion("1.0.0");
            projects.add(project2);
        }

        // Mock the MavenSession
        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(projects);

        return session;
    }

    @Test
    public void testExecuteWithNoExecFiles() throws Exception {
        assertNotNull("POM file not found", pom);
        assertTrue("POM file does not exist: " + pom.getAbsolutePath(), pom.exists());

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        assertNotNull("Mojo not found", mojo);

        // Create a real project without JaCoCo plugin
        Model model = new Model();
        model.setGroupId("test.group");
        model.setArtifactId("test-artifact");
        model.setVersion("1.0.0");
        MavenProject project = new MavenProject(model);
        mojo.project = project;

        // Create a session with this as the only project
        mojo.mavenSession = createMavenSession(project, true);

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
        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        assertNotNull("Mojo not found", mojo);

        // Create a project with JaCoCo plugin
        MavenProject project = createProjectWithJacocoPlugin(null);
        mojo.project = project;

        // Create a session with this as the only project
        mojo.mavenSession = createMavenSession(project, true);

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

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        assertNotNull("Mojo not found", mojo);

        // Create a real project
        Model model = new Model();
        model.setGroupId("test.group");
        model.setArtifactId("module1");
        model.setVersion("1.0.0");
        MavenProject project = new MavenProject(model);
        mojo.project = project;

        // Create a session with this as the first project in a multi-module build
        mojo.mavenSession = createMavenSession(project, false);

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

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        assertNotNull("Mojo not found", mojo);

        // Create the first project
        Model model1 = new Model();
        model1.setGroupId("test.group");
        model1.setArtifactId("module1");
        model1.setVersion("1.0.0");
        MavenProject project1 = new MavenProject(model1);

        // Create the last project (our current one)
        Model model2 = new Model();
        model2.setGroupId("test.group");
        model2.setArtifactId("module2");
        model2.setVersion("1.0.0");
        MavenProject project2 = new MavenProject(model2);
        mojo.project = project2;

        // Create a maven session with multiple projects
        MavenSession session = mock(MavenSession.class);
        List<MavenProject> projects = Arrays.asList(project1, project2);
        when(session.getProjects()).thenReturn(projects);
        mojo.mavenSession = session;

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

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        assertNotNull("Mojo not found", mojo);

        // Create a real project with JaCoCo plugin
        MavenProject project = createProjectWithJacocoPlugin(null);
        mojo.project = project;

        // Create a session with this as the only project
        mojo.mavenSession = createMavenSession(project, true);

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
        File baseDir = temporaryFolder.newFolder("baseDir");
        File module1 = new File(baseDir, "module1");
        File module2 = new File(baseDir, "module2");
        File target1 = new File(module1, "target");
        File target2 = new File(module2, "target");

        module1.mkdirs();
        module2.mkdirs();
        target1.mkdirs();
        target2.mkdirs();

        File execFile1 = new File(target1, "jacoco.exec");
        File execFile2 = new File(target2, "jacoco.exec");

        // Create empty files
        Files.createFile(execFile1.toPath());
        Files.createFile(execFile2.toPath());

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        assertNotNull("Mojo not found", mojo);

        // Create a real project with JaCoCo plugin
        MavenProject project = createProjectWithJacocoPlugin(null);
        mojo.project = project;

        // Create a session with this as the only project
        mojo.mavenSession = createMavenSession(project, true);

        // Configure mojo
        mojo.baseDir = baseDir;
        mojo.scanModules = true;
        mojo.jacocoExecFile = new File("nonexistent.exec");
        mojo.classesDirectory = testProjectClasses;

        // This should find and add the exec files, but since they're empty,
        // it will throw an exception when trying to read them
        try {
            mojo.execute();
            fail("Should throw exception with empty exec files");
        } catch (MojoExecutionException e) {
            // Expected
            assertTrue(e.getMessage().contains("Failed to process JaCoCo data"));
        }

        // Verify that both files were found
        assertEquals(2, mojo.additionalExecFiles.size());
        assertTrue(mojo.additionalExecFiles.contains(execFile1) || mojo.additionalExecFiles.contains(execFile2));
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
        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        assertNotNull("Mojo not found", mojo);

        // Create project with custom destFile
        MavenProject project = createProjectWithJacocoPlugin("${project.build.directory}/custom-coverage.exec");
        mojo.project = project;

        // Create a session with this as the only project
        mojo.mavenSession = createMavenSession(project, true);

        // Configure the mojo
        mojo.baseDir = baseDir;
        mojo.scanModules = true;
        mojo.jacocoExecFile = new File("nonexistent.exec");
        mojo.classesDirectory = testProjectClasses;

        // This should find the custom exec file, but since it's empty,
        // it will throw an exception trying to read it
        try {
            mojo.execute();
            fail("Should throw exception with empty exec files");
        } catch (MojoExecutionException e) {
            // Expected
        }

        // Verify the custom file was found
        boolean found = false;
        for (File file : mojo.additionalExecFiles) {
            if (file.getAbsolutePath().equals(customExec.getAbsolutePath())) {
                found = true;
                break;
            }
        }
        assertTrue("Custom exec file was not found", found);
    }

    @Test(expected = MojoExecutionException.class)
    public void testExecuteWithInvalidClassesDirectory() throws Exception {
        assertNotNull("POM file not found", pom);
        assertTrue("POM file does not exist: " + pom.getAbsolutePath(), pom.exists());

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        assertNotNull("Mojo not found", mojo);

        // Create a real project with JaCoCo plugin
        MavenProject project = createProjectWithJacocoPlugin(null);
        mojo.project = project;

        // Create a session with this as the only project
        mojo.mavenSession = createMavenSession(project, true);

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

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        assertNotNull("Mojo not found", mojo);

        // Create a real project with JaCoCo plugin
        MavenProject project = createProjectWithJacocoPlugin(null);
        mojo.project = project;

        // Create a session with this as the only project
        mojo.mavenSession = createMavenSession(project, true);

        mojo.jacocoExecFile = testProjectJacocoExec;
        mojo.classesDirectory = testProjectClasses;

        // Should execute without throwing an exception
        mojo.execute();
    }

    @Test
    public void testTruncateMiddle() throws Exception {
        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        assertNotNull("Mojo not found", mojo);

        // Use reflection to access the private method
        java.lang.reflect.Method truncateMethod = JacocoConsoleReporterMojo.class.getDeclaredMethod("truncateMiddle", String.class, int.class);
        truncateMethod.setAccessible(true);

        // Test normal case
        String longString = "com.example.very.long.package.name";
        String truncated = (String) truncateMethod.invoke(mojo, longString, 20);
        assertEquals("com.exam...age.name", truncated);

        // Test string already shorter than max
        String shortString = "com.example";
        String notTruncated = (String) truncateMethod.invoke(mojo, shortString, 20);
        assertEquals(shortString, notTruncated);
    }

    @Test
    public void testShouldReport() throws Exception {
        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        assertNotNull("Mojo not found", mojo);

        // Create two projects for a multi-module build
        MavenProject project1 = createProjectWithJacocoPlugin(null);
        project1.setGroupId("test.group");
        project1.setArtifactId("module1");
        project1.setVersion("1.0.0");

        MavenProject project2 = createProjectWithJacocoPlugin(null);
        project2.setGroupId("test.group");
        project2.setArtifactId("module2");
        project2.setVersion("1.0.0");

        // Create a maven session with both projects
        MavenSession session = mock(MavenSession.class);
        List<MavenProject> projects = Arrays.asList(project1, project2);
        when(session.getProjects()).thenReturn(projects);

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

    private String getBasedir() {
        return System.getProperty("basedir", new File("").getAbsolutePath());
    }
}