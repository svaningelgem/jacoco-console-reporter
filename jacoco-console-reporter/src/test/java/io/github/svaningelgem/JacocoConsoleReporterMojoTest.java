package io.github.svaningelgem;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;


public class JacocoConsoleReporterMojoTest extends BaseTestClass {
    /**
     * Creates a real MavenProject with JaCoCo plugin configuration
     */
    @Contract("_ -> new")
    private @NotNull MavenProject createProjectWithJacocoPlugin(String destFile) {
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
     * Creates a real MavenSession with multiple projects
     */
    private @NotNull MavenSession createRealMavenSession(List<MavenProject> projects) throws Exception {
        PlexusContainer container = rule.getContainer();
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        return new MavenSession(
                container,
                request,
                new DefaultMavenExecutionResult(),
                projects
        );
    }

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
        // Create a project with JaCoCo plugin
        MavenProject project = createProjectWithJacocoPlugin(null);
        mojo.project = project;

        // Create a real MavenSession with this as the only project
        List<MavenProject> projects = new ArrayList<>();
        projects.add(project);
        mojo.mavenSession = createRealMavenSession(projects);

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

        // Create a real project with JaCoCo plugin
        MavenProject project = createProjectWithJacocoPlugin(null);
        mojo.project = project;

        // Create a real MavenSession with this as the only project
        List<MavenProject> projects = new ArrayList<>();
        projects.add(project);
        mojo.mavenSession = createRealMavenSession(projects);

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

        // Create a real project with JaCoCo plugin
        MavenProject project = createProjectWithJacocoPlugin(null);
        mojo.project = project;

        // Create a real MavenSession with this as the only project
        List<MavenProject> projects = new ArrayList<>();
        projects.add(project);
        mojo.mavenSession = createRealMavenSession(projects);

        // Configure mojo
        mojo.baseDir = baseDir;
        mojo.scanModules = true;
        mojo.jacocoExecFile = new File("nonexistent.exec");
        mojo.classesDirectory = testProjectClasses;

        // Execute and verify files were found
        mojo.execute();

        // Verify that both files were found
        assertEquals(2, JacocoConsoleReporterMojo.collectedExecFilePaths.size());
        assertTrue(JacocoConsoleReporterMojo.collectedExecFilePaths.contains(execFile1) || JacocoConsoleReporterMojo.collectedExecFilePaths.contains(execFile2));
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

        // Create a real project with JaCoCo plugin
        MavenProject project = createProjectWithJacocoPlugin(null);
        mojo.project = project;

        // Create a real MavenSession with this as the only project
        List<MavenProject> projects = new ArrayList<>();
        projects.add(project);
        mojo.mavenSession = createRealMavenSession(projects);

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

        // Create a real project with JaCoCo plugin
        MavenProject project = createProjectWithJacocoPlugin(null);
        mojo.project = project;

        // Create a real MavenSession with this as the only project
        List<MavenProject> projects = new ArrayList<>();
        projects.add(project);
        mojo.mavenSession = createRealMavenSession(projects);

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

    @Test
    public void testPrintTreeWithShowFilesEnabled() {
        // Create a sample directory structure with files
        DirectoryNode root = new DirectoryNode("");
        createTree(root, 1, "pkg");

        // Set showFiles to true
        mojo.showFiles = true;

        // Print the tree directly
        root.printTree(mojo.getLog(), "", Defaults.LINE_FORMAT, "", true);

        String[] expected = {
                "pkg            ",
                "└─Example0.java",
        };
        assertLogContains(expected);
    }

    @Test
    public void testMultipleSubdirectoriesCase() {
        // Create a sample directory structure with multiple subdirectories
        DirectoryNode root = new DirectoryNode("");
        createTree(root, 1, "pkg1");
        createTree(root, 1, "pkg2");

        // Print the tree
        root.printTree(mojo.getLog(), "", Defaults.LINE_FORMAT, "", true);

        String[] expected = {
                "<root>           ",
                "├─pkg1           ",
                "│ └─Example0.java",
                "└─pkg2           ",
                "  └─Example1.java",
        };
        assertLogContains(expected);
    }

    @Test
    public void testPrintTreeWithNonEmptySubdirectory() {
        // Create a complex directory structure
        // root -> com -> example -> (file1.java, file2.java)
        DirectoryNode root = new DirectoryNode("");
        createTree(root, 2, "com", "example");

        // Print the tree
        root.printTree(mojo.getLog(), "", Defaults.LINE_FORMAT, "", true);

        String[] expected = {
                "com.example      ",
                "├─Example0.java  ",
                "└─Example1.java  "
        };
        assertLogContains(expected);
    }

    @Test
    public void testPrintTreeWithCollapsedDirectoryPath() {
        // Create a directory structure perfect for collapsing
        // com -> example -> util -> (Util.java)
        DirectoryNode root = new DirectoryNode("");
        createTree(root, 1, "com", "example", "util");

        // Print the tree - should collapse the path
        root.printTree(mojo.getLog(), "", Defaults.LINE_FORMAT, "", true);

        String[] expected = {
                "com.example.util ",
                "└─Example0.java  "
        };
        assertLogContains(expected);    }

    @Test
    public void testPrintTreeWithSourceFilesAndMultipleSubdirs() {
        // Create a complex case with both source files and multiple subdirectories
        DirectoryNode root = new DirectoryNode("");

        createTree(root, 1, "com", "example", "model");
        createTree(root, 1, "com", "example", "util");
        createTree(root, 1, "com", "example");

        // Enable showing files
        mojo.showFiles = true;

        // Print the tree
        root.printTree(mojo.getLog(), "", Defaults.LINE_FORMAT, "", true);

        // Test output should match the expected format for com.example
        String[] expected = {
                "com.example       ",
                "├─model           ",
                "│ └─Example0.java ",
                "├─util            ",
                "│ └─Example1.java ",
                "└─Example2.java   "
        };
        assertLogContains(expected);
    }

    @Test
    public void testPrintTreeWithSourceFilesAndMultipleSubdirs2() {
        // Create a complex case with both source files and multiple subdirectories
        DirectoryNode root = new DirectoryNode("");
        createTree(root, 1, "com", "example", "model");
        createTree(root, 1, "com", "example", "util");
        createTree(root, 0, "com", "example", "dummy"); // No files --> shouldn't show this!

        // Enable showing files
        mojo.showFiles = true;

        // Print the tree
        root.printTree(mojo.getLog(), "", Defaults.LINE_FORMAT, "", true);

        String[] expected = {
                "com.example       ",
                "├─model           ",
                "│ └─Example0.java ",
                "└─util            ",
                "  └─Example1.java ",
        };

        assertLogContains(expected);
    }
}