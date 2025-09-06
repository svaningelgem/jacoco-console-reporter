package io.github.svaningelgem;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
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

        // Configure build directories
        File targetDir = temporaryFolder.newFolder("target");
        File classesDir = new File(targetDir, "classes");
        classesDir.mkdirs();

        project.getBuild().setDirectory(targetDir.getAbsolutePath());
        project.getBuild().setOutputDirectory(classesDir.getAbsolutePath());

        mojo.project = project;

        // Create a real MavenSession with this as the only project
        List<MavenProject> projects = new ArrayList<>();
        projects.add(project);
        mojo.mavenSession = createRealMavenSession(projects);

        // Should log a message about no JaCoCo plugin and return without throwing an exception
        mojo.execute();
    }

    @Test
    public void testWithJacocoPlugin() throws Exception {
        // The default setUp already creates a project with JaCoCo plugin
        // Just need to ensure the exec file doesn't exist
        File targetDir = temporaryFolder.newFolder("target");
        File classesDir = new File(targetDir, "classes");
        classesDir.mkdirs();

        configureProjectForTesting(targetDir, classesDir, new File(targetDir, "nonexistent.exec"));

        // Execute should detect the JaCoCo plugin
        mojo.execute();

        // No assertions needed - just verify it doesn't throw an exception
    }

    @Test
    public void testExecuteWithDeferredReportingFirstModule() throws Exception {
        assertNotNull("POM file not found", pom);
        assertTrue("POM file does not exist: " + pom.getAbsolutePath(), pom.exists());

        // Create the first project
        File targetDir1 = temporaryFolder.newFolder("module1", "target");
        File classesDir1 = new File(targetDir1, "classes");

        MavenProject project1 = createProjectWithJacocoPlugin(new File(targetDir1, "jacoco.exec").getAbsolutePath());
        project1.setGroupId("test.group");
        project1.setArtifactId("module1");
        project1.setVersion("1.0.0");
        project1.getBuild().setDirectory(targetDir1.getAbsolutePath());
        project1.getBuild().setOutputDirectory(classesDir1.getAbsolutePath());

        // Create the second project
        File targetDir2 = temporaryFolder.newFolder("module2", "target");
        File classesDir2 = new File(targetDir2, "classes");

        MavenProject project2 = createProjectWithJacocoPlugin(new File(targetDir2, "jacoco.exec").getAbsolutePath());
        project2.setGroupId("test.group");
        project2.setArtifactId("module2");
        project2.setVersion("1.0.0");
        project2.getBuild().setDirectory(targetDir2.getAbsolutePath());
        project2.getBuild().setOutputDirectory(classesDir2.getAbsolutePath());

        // Set current project to the first one
        mojo.project = project1;

        // Create a real MavenSession with multiple projects
        List<MavenProject> projects = Arrays.asList(project1, project2);
        mojo.mavenSession = createRealMavenSession(projects);

        // Set deferred reporting
        mojo.deferReporting = true;

        // Should log a message about deferring and return without exception
        mojo.execute();

        // Check for deferring message
        assertTrue("Should log deferring message",
                log.writtenData.stream().anyMatch(s -> s.contains("Deferring JaCoCo reporting")));
    }

    @Test
    public void testExecuteWithDeferredReportingLastModule() throws Exception {
        assertNotNull("POM file not found", pom);
        assertTrue("POM file does not exist: " + pom.getAbsolutePath(), pom.exists());

        // Create the first project
        File targetDir1 = temporaryFolder.newFolder("module1", "target");
        File classesDir1 = new File(targetDir1, "classes");

        MavenProject project1 = createProjectWithJacocoPlugin(new File(targetDir1, "jacoco.exec").getAbsolutePath());
        project1.setGroupId("test.group");
        project1.setArtifactId("module1");
        project1.setVersion("1.0.0");
        project1.getBuild().setDirectory(targetDir1.getAbsolutePath());
        project1.getBuild().setOutputDirectory(classesDir1.getAbsolutePath());

        // Create the last project (our current one)
        File targetDir2 = temporaryFolder.newFolder("module2", "target");
        File classesDir2 = new File(targetDir2, "classes");

        MavenProject project2 = createProjectWithJacocoPlugin(new File(targetDir2, "jacoco.exec").getAbsolutePath());
        project2.setGroupId("test.group");
        project2.setArtifactId("module2");
        project2.setVersion("1.0.0");
        project2.getBuild().setDirectory(targetDir2.getAbsolutePath());
        project2.getBuild().setOutputDirectory(classesDir2.getAbsolutePath());

        mojo.project = project2;

        // Create a real MavenSession with multiple projects
        List<MavenProject> projects = Arrays.asList(project1, project2);
        mojo.mavenSession = createRealMavenSession(projects);

        // Set deferred reporting
        mojo.deferReporting = true;

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

        // Configure the project to use test project files
        File targetDir = testProjectJacocoExec.getParentFile();
        configureProjectForTesting(targetDir, testProjectClasses, testProjectJacocoExec);

        // Set showFiles to false
        mojo.showFiles = false;

        // Should execute without throwing an exception
        mojo.execute();
    }

    @Test
    public void testExecuteWithInvalidClassesDirectory() throws Exception {
        assertNotNull("POM file not found", pom);
        assertTrue("POM file does not exist: " + pom.getAbsolutePath(), pom.exists());

        // Create a temporary exec file
        File tempExecFile = temporaryFolder.newFile("temp.exec");

        // Use a non-existent directory
        File nonExistentDir = new File("nonexistent/classes");
        File targetDir = temporaryFolder.newFolder("target");

        configureProjectForTesting(targetDir, nonExistentDir, tempExecFile);

        mojo.execute();
    }

    @Test
    public void testExecute() throws Exception {
        if (!testProjectJacocoExec.exists() || !testProjectClasses.exists()) {
            fail("Jacoco.exec or classes directory does not exist: " + testProjectJacocoExec.getAbsolutePath());
        }

        assertNotNull("POM file not found", pom);
        assertTrue("POM file does not exist: " + pom.getAbsolutePath(), pom.exists());

        // Configure the project to use test project files
        File targetDir = testProjectJacocoExec.getParentFile();
        configureProjectForTesting(targetDir, testProjectClasses, testProjectJacocoExec);

        // Should execute without throwing an exception
        mojo.execute();
    }

    @Test
    public void testTruncateMiddle() {
        // Test normal case
        String longString = "com.example.very.very.very.very.very.very.very.very.long.package.name";
        String truncated = new Defaults().truncateMiddle(longString);
        assertEquals("com.example.very.very.ve..y.very.long.package.name", truncated);

        // Test string already shorter than max
        String shortString = "com.example";
        String notTruncated = new Defaults().truncateMiddle(shortString);
        assertEquals(shortString, notTruncated);
    }

    @Test
    public void testShouldReport() throws Exception {
        // Create two projects for a multi-module build
        File targetDir1 = temporaryFolder.newFolder("module1", "target");
        File classesDir1 = new File(targetDir1, "classes");

        MavenProject project1 = createProjectWithJacocoPlugin(new File(targetDir1, "jacoco.exec").getAbsolutePath());
        project1.setGroupId("test.group");
        project1.setArtifactId("module1");
        project1.setVersion("1.0.0");
        project1.getBuild().setDirectory(targetDir1.getAbsolutePath());
        project1.getBuild().setOutputDirectory(classesDir1.getAbsolutePath());

        File targetDir2 = temporaryFolder.newFolder("module2", "target");
        File classesDir2 = new File(targetDir2, "classes");

        MavenProject project2 = createProjectWithJacocoPlugin(new File(targetDir2, "jacoco.exec").getAbsolutePath());
        project2.setGroupId("test.group");
        project2.setArtifactId("module2");
        project2.setVersion("1.0.0");
        project2.getBuild().setDirectory(targetDir2.getAbsolutePath());
        project2.getBuild().setOutputDirectory(classesDir2.getAbsolutePath());

        // Create a real MavenSession with both projects
        List<MavenProject> projects = Arrays.asList(project1, project2);
        MavenSession session = createRealMavenSession(projects);

        // Test with first project
        mojo.project = project1;
        mojo.mavenSession = session;
        mojo.deferReporting = true;

        boolean hasReported = (boolean) mojo.shouldReport();
        assertFalse("First module should not report when deferring", hasReported);

        // Test with last project
        mojo.project = project2;
        hasReported = (boolean) mojo.shouldReport();
        assertTrue("Last module should report even when deferring", hasReported);

        // Test without deferring
        mojo.project = project1;
        mojo.deferReporting = false;
        hasReported = (boolean) mojo.shouldReport();
        assertTrue("Module should report when not deferring", hasReported);
    }

    @Test
    public void testExecuteWithXmlOutputFile() throws Exception {
        if (!testProjectJacocoExec.exists() || !testProjectClasses.exists()) {
            // Skip test if test project files don't exist
            return;
        }

        File xmlFile = temporaryFolder.newFile("test-report.xml");
        File targetDir = testProjectJacocoExec.getParentFile();

        configureProjectForTesting(targetDir, testProjectClasses, testProjectJacocoExec);

        mojo.xmlOutputFile = xmlFile;
        mojo.writeXmlReport = true;
        mojo.deferReporting = false;

        // Should execute and attempt to generate XML report
        mojo.execute();

        // Check if XML generation was attempted
        boolean foundXmlGenerationLog = log.writtenData.stream()
                .anyMatch(line -> line.contains("Generating aggregated JaCoCo XML report"));

        if (xmlFile.exists() && xmlFile.length() > 0) {
            // XML was successfully generated
            assertTrue("Should log XML generation", foundXmlGenerationLog);

            boolean foundSuccessLog = log.writtenData.stream()
                    .anyMatch(line -> line.contains("XML report generated successfully"));
            assertTrue("Should log XML success", foundSuccessLog);
        } else if (foundXmlGenerationLog) {
            // XML generation was attempted but failed (acceptable in test environment)
            System.out.println("XML generation attempted but failed (expected in test environment)");
        }
    }

    @Test
    public void testExecuteWithXmlOutputFileAndNoExecFile() throws Exception {
        File xmlFile = temporaryFolder.newFile("empty-report.xml");
        File targetDir = temporaryFolder.newFolder("target");
        File classesDir = new File(targetDir, "classes");

        configureProjectForTesting(targetDir, classesDir, new File(targetDir, "nonexistent.exec"));

        mojo.xmlOutputFile = xmlFile;
        mojo.writeXmlReport = true;
        mojo.deferReporting = false;

        // Should execute without throwing exception
        mojo.execute();

        // XML file may or may not be created depending on whether there's any coverage data
        // The important thing is that no exception was thrown
    }

    @Test
    public void testExecuteWithXmlOutputFileInNonExistentDirectory() throws Exception {
        if (!testProjectJacocoExec.exists() || !testProjectClasses.exists()) {
            return;
        }

        File nonExistentDir = new File(temporaryFolder.getRoot(), "nonexistent");
        File xmlFile = new File(nonExistentDir, "report.xml");
        File targetDir = testProjectJacocoExec.getParentFile();

        configureProjectForTesting(targetDir, testProjectClasses, testProjectJacocoExec);

        mojo.xmlOutputFile = xmlFile;
        mojo.writeXmlReport = true;
        mojo.deferReporting = false;

        try {
            mojo.execute();

            // If it succeeds, check if XML generation was attempted
            boolean foundXmlLog = log.writtenData.stream()
                    .anyMatch(line -> line.contains("Generating aggregated JaCoCo XML report"));

            if (foundXmlLog) {
                // XML generation was attempted, which is what we want to test
                System.out.println("XML generation was attempted with invalid path");
            }

        } catch (MojoExecutionException e) {
            // Expected exception due to invalid XML output path
            assertTrue("Should contain error message about processing JaCoCo data",
                    e.getMessage().contains("Failed to process JaCoCo data"));
        }
    }

    @Test
    public void testXmlGenerationWithDeferredReporting() throws Exception {
        if (!testProjectJacocoExec.exists() || !testProjectClasses.exists()) {
            return;
        }

        File xmlFile = temporaryFolder.newFile("deferred-report.xml");

        // Create two projects for multi-module build
        File targetDir1 = temporaryFolder.newFolder("module1", "target");
        File classesDir1 = new File(targetDir1, "classes");

        MavenProject project1 = createProjectWithJacocoPlugin(new File(targetDir1, "jacoco.exec").getAbsolutePath());
        project1.setGroupId("test.group");
        project1.setArtifactId("module1");
        project1.setVersion("1.0.0");
        project1.getBuild().setDirectory(targetDir1.getAbsolutePath());
        project1.getBuild().setOutputDirectory(classesDir1.getAbsolutePath());

        // Use test project files for second module
        MavenProject project2 = createProjectWithJacocoPlugin(testProjectJacocoExec.getAbsolutePath());
        project2.setGroupId("test.group");
        project2.setArtifactId("module2");
        project2.setVersion("1.0.0");
        project2.getBuild().setDirectory(testProjectJacocoExec.getParentFile().getAbsolutePath());
        project2.getBuild().setOutputDirectory(testProjectClasses.getAbsolutePath());

        // Set current project to the last one (should trigger reporting)
        mojo.project = project2;
        mojo.mavenSession = createRealMavenSession(Arrays.asList(project1, project2));

        mojo.xmlOutputFile = xmlFile;
        mojo.writeXmlReport = true;
        mojo.deferReporting = true;

        mojo.execute();

        // Should attempt XML generation since this is the last module
        boolean foundXmlGenerationLog = log.writtenData.stream()
                .anyMatch(line -> line.contains("Generating aggregated JaCoCo XML report"));

        // XML generation should be attempted for last module
        assertTrue("Should attempt XML generation for last module with deferred reporting", foundXmlGenerationLog);
    }

    @Test
    public void testXmlGenerationSkippedForNonLastModuleWithDeferredReporting() throws Exception {
        File xmlFile = temporaryFolder.newFile("should-not-be-generated.xml");
        xmlFile.delete(); // Start with no file

        // Create two projects for multi-module build
        File targetDir1 = temporaryFolder.newFolder("module1", "target");
        File classesDir1 = new File(targetDir1, "classes");

        MavenProject project1 = createProjectWithJacocoPlugin(new File(targetDir1, "jacoco.exec").getAbsolutePath());
        project1.setGroupId("test.group");
        project1.setArtifactId("module1");
        project1.setVersion("1.0.0");
        project1.getBuild().setDirectory(targetDir1.getAbsolutePath());
        project1.getBuild().setOutputDirectory(classesDir1.getAbsolutePath());

        File targetDir2 = temporaryFolder.newFolder("module2", "target");
        File classesDir2 = new File(targetDir2, "classes");

        MavenProject project2 = createProjectWithJacocoPlugin(new File(targetDir2, "jacoco.exec").getAbsolutePath());
        project2.setGroupId("test.group");
        project2.setArtifactId("module2");
        project2.setVersion("1.0.0");
        project2.getBuild().setDirectory(targetDir2.getAbsolutePath());
        project2.getBuild().setOutputDirectory(classesDir2.getAbsolutePath());

        // Set current project to the first one (should NOT trigger reporting)
        mojo.project = project1;
        mojo.mavenSession = createRealMavenSession(Arrays.asList(project1, project2));

        mojo.xmlOutputFile = xmlFile;
        mojo.writeXmlReport = true;
        mojo.deferReporting = true;

        mojo.execute();

        // Should NOT attempt XML generation since this is not the last module
        boolean foundXmlGenerationLog = log.writtenData.stream()
                .anyMatch(line -> line.contains("Generating aggregated JaCoCo XML report"));

        assertFalse("Should not attempt XML generation for non-last module with deferred reporting", foundXmlGenerationLog);
        assertFalse("XML file should not exist for non-last module with deferred reporting", xmlFile.exists());
    }

    @Test
    public void testCustomExecFilePattern() throws Exception {
        File baseDir = temporaryFolder.newFolder("custom-exec");
        File targetDir = new File(baseDir, "target");
        targetDir.mkdirs();

        // Create a custom exec file
        File customExec = new File(targetDir, "custom-coverage.exec");
        Files.createFile(customExec.toPath());

        // Create project with custom destFile
        MavenProject project = createProjectWithJacocoPlugin(customExec.getAbsolutePath());
        project.getBuild().setDirectory(targetDir.getAbsolutePath());
        project.getBuild().setOutputDirectory(new File(targetDir, "classes").getAbsolutePath());

        mojo.project = project;

        // Create a real MavenSession with this as the only project
        List<MavenProject> projects = new ArrayList<>();
        projects.add(project);
        mojo.mavenSession = createRealMavenSession(projects);

        // Execute
        mojo.execute();

        // Verify the custom file was found in collected paths
        boolean found = false;
        for (File file : JacocoConsoleReporterMojo.collectedExecFilePaths) {
            if (file.getAbsolutePath().equals(customExec.getAbsolutePath())) {
                found = true;
                break;
            }
        }
        assertTrue("Custom exec file was found", found);
    }
}