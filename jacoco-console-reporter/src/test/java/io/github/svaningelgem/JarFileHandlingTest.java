package io.github.svaningelgem;

import lombok.var;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static io.github.svaningelgem.JacocoConsoleReporterMojo.collectedClassesPaths;
import static io.github.svaningelgem.JacocoConsoleReporterMojo.collectedExecFilePaths;
import static org.junit.Assert.*;

public class JarFileHandlingTest extends BaseTestClass {
    /**
     * Test that the analyzer correctly ignores JAR files and exploded JAR directories
     */
    @Test
    public void testAnalyzeCoverageIgnoresJarFiles() throws Exception {
        // Skip test if test project files don't exist
        assertTrue("We need the test projects jacoco.exec!", testProjectJacocoExec.exists());
        assertTrue("We need the main projects compiled files!", mainProjectClasses.exists());

        // Create a temporary directory structure to hold test classes and jar
        File testClassesDir = temporaryFolder.newFolder("test-classes");

        // Copy a real JAR file from resources to the test directory
        File jarFile = new File(testClassesDir, "test.jar");
        copyResourceToFile("/sample-classes.jar", jarFile);

        // Verify files exist
        assertTrue("JAR file should exist", jarFile.exists());

        // Copy files from test-project classes to our test directory
        copyDirectory(mainProjectClasses, testClassesDir);

        // Set up the mojo for testing
        mojo.jacocoExecFile = testProjectJacocoExec;
        mojo.classesDirectory = testClassesDir;
        mojo.showSummary = false;

        collectedExecFilePaths.add(testProjectJacocoExec);
        collectedClassesPaths.add(testClassesDir);

        var generateReport = JacocoConsoleReporterMojo.class.getDeclaredMethod("generateReport");
        generateReport.setAccessible(true);

        generateReport.invoke(mojo);

        String[] expected = {
                "<root>                  ",
                "└─io.github.svaningelgem",
        };
        assertLogContains(expected);
    }
}