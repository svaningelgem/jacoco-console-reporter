package com.github;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.MojoRule;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class JacocoConsoleReporterMojoTest {

    @Rule
    public final MojoRule rule = new MojoRule();

    private final File testProjectDir = new File("../test-project");
    private final File testProjectJacocoExec = new File(testProjectDir, "target/jacoco.exec");
    private final File testProjectClasses = new File(testProjectDir, "target/classes");
    private final File pom = new File(getBasedir(),"src/test/resources/unit/pom.xml");

    @Test
    public void testExecuteWithMissingExecFile() throws Exception {
        assertNotNull("POM file not found", pom);
        assertTrue("POM file does not exist: " + pom.getAbsolutePath(), pom.exists());

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        assertNotNull("Mojo not found", mojo);

        // Set a non-existent jacoco.exec file
        File execFile = new File("target/nonexistent.exec");
        File classesDir = new File("target/classes");
        classesDir.mkdirs();

        mojo.jacocoExecFile = execFile;
        mojo.classesDirectory = classesDir;

        // Should log a warning and return without throwing an exception
        mojo.execute();
    }

    @Test(expected = MojoExecutionException.class)
    public void testExecuteWithInvalidClassesDirectory() throws Exception {
        assertNotNull("POM file not found", pom);
        assertTrue("POM file does not exist: " + pom.getAbsolutePath(), pom.exists());

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        assertNotNull("Mojo not found", mojo);

        // Use the real JaCoCo execution data but with an invalid classes directory
        File nonExistentDir = new File("nonexistent/classes");

        mojo.jacocoExecFile = testProjectJacocoExec;
        mojo.classesDirectory = nonExistentDir;

        mojo.execute();
    }

    @Test
    public void testExecute() throws Exception {
        assertNotNull("POM file not found", pom);
        assertTrue("POM file does not exist: " + pom.getAbsolutePath(), pom.exists());

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        assertNotNull("Mojo not found", mojo);

        assertTrue("Test project JaCoCo exec file not found", testProjectJacocoExec.exists());
        assertTrue("Test project classes directory not found", testProjectClasses.exists());

        mojo.jacocoExecFile = testProjectJacocoExec;
        mojo.classesDirectory = testProjectClasses;

        mojo.execute();
    }

    private String getBasedir() {
        return System.getProperty("basedir", new File("").getAbsolutePath());
    }
}