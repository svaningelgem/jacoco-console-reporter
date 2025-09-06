package io.github.svaningelgem;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

        // Configure project to use our classes directory
        File targetDir = temporaryFolder.newFolder("target");
        configureProjectForTesting(targetDir, classesDir, null);

        // Execute to initialize targetDir and baseDir
        mojo.execute();

        // Clear and add this to the mojo's class paths
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
    public void testAddBuildDirExclusionEnabled() throws IOException, MojoExecutionException {
        assertEquals(0, JacocoConsoleReporterMojo.collectedExcludePatterns.size());
        mojo.ignoreFilesInBuildDirectory = true;

        // Set up project directories
        File targetDir = temporaryFolder.newFolder("target");
        File classesDir = new File(targetDir, "classes");
        configureProjectForTesting(targetDir, classesDir, null);

        // Execute to initialize targetDir
        mojo.execute();

        // Create a few files in the target directory
        createFile(targetDir, "classes/com/example/TestClass.java", "package io.sample;");
        createFile(targetDir, "classes/com/sth/Else.java", "not-a-package io.sample;");
        createFile(targetDir, "classes/com/example/ProductionClass.class", "package io.sample2;");

        mojo.addBuildDirExclusion();

        Set<String> patterns = JacocoConsoleReporterMojo.collectedExcludePatterns.stream().map(Pattern::pattern).collect(Collectors.toSet());
        assertTrue(patterns.contains("^io/sample/TestClass$"));
    }

    @Test
    public void testThrowingExceptionDuringCanonicalPath() throws IOException, MojoExecutionException {
        // Set up project directories
        File targetDir = temporaryFolder.newFolder("target");
        File classesDir = new File(targetDir, "classes");
        configureProjectForTesting(targetDir, classesDir, null);

        // Execute to initialize targetDir
        mojo.execute();

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
    public void testThrowingExceptionDuringReadingFile() throws IOException, MojoExecutionException {
        // Set up project directories
        File targetDir = temporaryFolder.newFolder("target");
        File classesDir = new File(targetDir, "classes");
        configureProjectForTesting(targetDir, classesDir, null);

        // Execute to initialize targetDir
        mojo.execute();

        mojo.fileReader = new FileReader() {
            @Override
            public String readAllBytes(Path path, Charset cs) throws IOException {
                throw new IOException("boom");
            }
        };

        createFile(targetDir, "classes/com/example/TestClass.java", "package io.sample;");
        mojo.addBuildDirExclusion();

        String[] expected = {"[warn] Failed to read file: "};
        assertLogContains(expected);
    }

    @Test
    public void testAddBuildDirExclusionDisabled() throws Exception {
        assertEquals(0, JacocoConsoleReporterMojo.collectedExcludePatterns.size());
        mojo.ignoreFilesInBuildDirectory = false;

        // Set up project directories
        File targetDir = temporaryFolder.newFolder("target");
        File classesDir = new File(targetDir, "classes");
        configureProjectForTesting(targetDir, classesDir, null);

        // Execute to initialize targetDir
        mojo.execute();

        mojo.addBuildDirExclusion();
        assertEquals(3, JacocoConsoleReporterMojo.collectedExcludePatterns.size());
    }
}