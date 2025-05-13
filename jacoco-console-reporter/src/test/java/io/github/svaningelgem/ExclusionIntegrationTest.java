package io.github.svaningelgem;

import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ExclusionIntegrationTest extends BaseTestClass {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Create a JaCoCo plugin with exclusions
        Plugin jacocoPlugin = new Plugin();
        jacocoPlugin.setGroupId("org.jacoco");
        jacocoPlugin.setArtifactId("jacoco-maven-plugin");

        // Create configuration with excludes
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom excludes = new Xpp3Dom("excludes");

        // Add exclusion patterns
        String[] excludePatterns = {
                "com/example/generated/**/*",
                "**/*Controller.class",
                "**/model/*"
        };

        for (String pattern : excludePatterns) {
            Xpp3Dom exclude = new Xpp3Dom("exclude");
            exclude.setValue(pattern);
            excludes.addChild(exclude);
        }

        configuration.addChild(excludes);
        jacocoPlugin.setConfiguration(configuration);

        // Add to the project's build plugins
        mojo.project.getBuild().getPlugins().clear();
        mojo.project.getBuild().addPlugin(jacocoPlugin);
    }

    @Test
    public void testExclusionFromAnalysis() throws Exception {
        // Create a fake directory structure for class file analysis
        File classesDir = temporaryFolder.newFolder("classes");
        File generatedDir = new File(classesDir, "com/example/generated");
        generatedDir.mkdirs();

        // Add this to the mojo's class paths
        mojo.classesDirectory = classesDir;
        JacocoConsoleReporterMojo.collectedClassesPaths.clear();
        JacocoConsoleReporterMojo.collectedClassesPaths.add(classesDir);

        // First, load the exclusion patterns
        mojo.ignoreFilesInBuildDirectory = true;
        mojo.loadExclusionPatterns();

        // Verify exclusion patterns are working
        assertTrue(mojo.isExcluded("com/example/generated/api/UsersApi"));
        assertTrue(mojo.isExcluded("com/example/web/UserController"));
        assertTrue(mojo.isExcluded("com/example/model/User"));
        assertFalse(mojo.isExcluded("com/example/Calculator"));
    }

    @Test
    public void testExclusionWithAdditionalPatterns() {
        // Add explicit excludes to the mojo as well
        List<String> additionalExcludes = Arrays.asList(
                "com/example/ignored/**/*",
                "**/*Test.class",
                "**/*DTO",
                "com/example/ignored2",
                "com/example/ignored3/"
        );

        // Load the patterns
        mojo.loadExclusionPatterns();
        additionalExcludes.forEach(mojo::addExclusion);

        // Test combined exclusions
        assertTrue(mojo.isExcluded("com/example/generated/model/User"));
        assertTrue(mojo.isExcluded("com/example/ignored/SomeClass"));
        assertTrue(mojo.isExcluded("com/example/service/UserServiceTest"));
        assertTrue(mojo.isExcluded("testing/UserDTO"));
        assertFalse(mojo.isExcluded("testing/Userdto"));
        assertFalse(mojo.isExcluded("com/example/ignored2/SomeClass"));
        assertFalse(mojo.isExcluded("com/example/ignored3/SomeClass"));
    }

    @Test
    public void testAddBuildDirExclusionEnabled() throws IOException {
        assertEquals(0, JacocoConsoleReporterMojo.collectedExcludePatterns.size());
        mojo.ignoreFilesInBuildDirectory = true;

        // Create a few files in the target directory
        createFile(mojo.targetDir, "classes/com/example/TestClass.java", "package io.sample;");
        createFile(mojo.targetDir, "classes/com/sth/Else.java", "not-a-package io.sample;");
        createFile(mojo.targetDir, "classes/com/example/ProductionClass.class", "package io.sample2;");

        mojo.addBuildDirExclusion();

        assertPatternEquals(Collections.singletonList("^io/sample/TestClass$"), JacocoConsoleReporterMojo.collectedExcludePatterns);
    }

    @Test
    public void testThrowingExceptionDuringCanonicalPath() throws IOException {
        mojo.fileReader = new FileReader() {
            @Override
            public String canonicalPath(@NotNull File f) throws IOException {
                throw new IOException("boom");
            }
        };

        mojo.addBuildDirExclusion();

        String[] expected = {"[warn] Failed to add build directory exclusion: boom"};
        assertLogContains(expected);
    }

    @Test
    public void testThrowingExceptionDuringReadingFile() throws IOException {
        mojo.fileReader = new FileReader() {
            @Override
            public String readAllBytes(Path path, Charset cs) throws IOException {
                throw new IOException("boom");
            }
        };

        createFile(mojo.targetDir, "classes/com/example/TestClass.java", "package io.sample;");
        mojo.addBuildDirExclusion();

        String[] expected = {"[warn] Failed to read file: "};
        assertLogContains(expected);
    }

    @Test
    public void testAddBuildDirExclusionDisabled() {
        assertEquals(0, JacocoConsoleReporterMojo.collectedExcludePatterns.size());
        mojo.ignoreFilesInBuildDirectory = false;
        mojo.addBuildDirExclusion();
        assertEquals(0, JacocoConsoleReporterMojo.collectedExcludePatterns.size());
    }
}