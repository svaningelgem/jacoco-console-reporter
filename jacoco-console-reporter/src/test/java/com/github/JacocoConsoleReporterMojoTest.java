package com.github;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.*;

public class JacocoConsoleReporterMojoTest {

    @Rule
    public MojoRule rule = new MojoRule();

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private final File testProjectDir = new File("../test-project");
    private final File testProjectJacocoExec = new File(testProjectDir, "target/jacoco.exec");
    private final File testProjectClasses = new File(testProjectDir, "target/classes");

    @Before
    public void setUp() throws Exception {
        // Ensure test project is built and JaCoCo has run
        assertTrue("Test project directory not found", testProjectDir.exists());
        assertTrue("JaCoCo execution data not found. Did you run 'mvn test' on test-project?", testProjectJacocoExec.exists());
        assertTrue("Compiled classes not found. Did you run 'mvn test' on test-project?", testProjectClasses.exists());
    }

    @Test
    public void testExecuteWithMissingExecFile() throws Exception {
        URL pomUrl = getClass().getResource("/test-pom.xml");
        assertNotNull("test-pom.xml not found in test resources", pomUrl);
        File pom = new File(pomUrl.toURI());
        assertTrue(pom.exists());

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupMojo("report", pom);
        assertNotNull("Mojo not found", mojo);

        // Set a non-existent jacoco.exec file
        File execFile = tempDir.newFile("nonexistent.exec");
        File classesDir = tempDir.newFolder("classes");

        mojo.jacocoExecFile = execFile;
        mojo.classesDirectory = classesDir;

        // Should log a warning and return without throwing an exception
        mojo.execute();
    }

    @Test(expected = MojoExecutionException.class)
    public void testExecuteWithInvalidClassesDirectory() throws Exception {
        URL pomUrl = getClass().getResource("/test-pom.xml");
        assertNotNull("test-pom.xml not found in test resources", pomUrl);
        File pom = new File(pomUrl.toURI());
        assertTrue(pom.exists());

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupMojo("report", pom);
        assertNotNull("Mojo not found", mojo);

        // Use the real JaCoCo execution data but with an invalid classes directory
        File nonExistentDir = tempDir.newFolder();
        nonExistentDir.delete(); // Make it non-existent after creation

        mojo.jacocoExecFile = testProjectJacocoExec;
        mojo.classesDirectory = nonExistentDir;

        mojo.execute();
    }

    @Test
    public void testExecute() throws Exception {
        URL pomUrl = getClass().getResource("/test-pom.xml");
        assertNotNull("test-pom.xml not found in test resources", pomUrl);
        File pom = new File(pomUrl.toURI());
        assertTrue(pom.exists());

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupMojo("report", pom);
        assertNotNull("Mojo not found", mojo);

        mojo.jacocoExecFile = testProjectJacocoExec;
        mojo.classesDirectory = testProjectClasses;

        mojo.execute();
    }
}