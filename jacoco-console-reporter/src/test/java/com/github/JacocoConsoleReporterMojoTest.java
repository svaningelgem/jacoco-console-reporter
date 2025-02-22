package com.github;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class JacocoConsoleReporterMojoTest extends AbstractMojoTestCase {

    private MojoRule rule = new MojoRule();

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testExecuteWithMissingExecFile() throws Exception {
        File pom = getTestFile("src/test/resources/test-pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom, "report");
        assertNotNull(mojo);

        rule.setVariableValueToObject(mojo, "jacocoExecFile", new File("target/nonexistent.exec"));
        rule.setVariableValueToObject(mojo, "classesDirectory", new File("target/classes"));

        mojo.execute();
    }

    @Test
    public void testExecuteWithInvalidClassesDirectory() {
        File pom = getTestFile("src/test/resources/test-pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom, "report");
        assertNotNull(mojo);

        rule.setVariableValueToObject(mojo, "jacocoExecFile", new File("src/test/resources/sample-jacoco.exec"));
        rule.setVariableValueToObject(mojo, "classesDirectory", new File("nonexistent/classes"));

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    public void testExecute() throws Exception {
        File testProjectDir = new File("../test-project");
        File pom = new File(testProjectDir, "pom.xml");
        assertTrue(pom.exists());

        File jacocoExecFile = new File(testProjectDir, "target/jacoco.exec");
        assertTrue(jacocoExecFile.exists());

        File classesDirectory = new File(testProjectDir, "target/classes");
        assertTrue(classesDirectory.exists());

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom, "report");
        mojo.setJacocoExecFile(jacocoExecFile);
        mojo.setClassesDirectory(classesDirectory);

        mojo.execute();
    }
}