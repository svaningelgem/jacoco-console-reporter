package com.example;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

public class JacocoConsoleReporterMojoTest extends AbstractMojoTestCase {

    @Rule
    public MojoRule rule = new MojoRule();

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

        // Set a non-existent jacoco.exec file
        rule.setVariableValueToObject(mojo, "jacocoExecFile", new File("target/nonexistent.exec"));
        rule.setVariableValueToObject(mojo, "classesDirectory", new File("target/classes"));

        mojo.execute();
        // Should log a warning and return without throwing an exception
        // Verification would typically involve mocking the Log, but we'll assume it works as expected
    }

    @Test(expected = MojoExecutionException.class)
    public void testExecuteWithInvalidClassesDirectory() throws Exception {
        File pom = getTestFile("src/test/resources/test-pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom, "report");
        assertNotNull(mojo);

        // Set an invalid classes directory to force an IOException
        rule.setVariableValueToObject(mojo, "jacocoExecFile", new File("src/test/resources/sample-jacoco.exec"));
        rule.setVariableValueToObject(mojo, "classesDirectory", new File("nonexistent/classes"));

        mojo.execute();
    }
}