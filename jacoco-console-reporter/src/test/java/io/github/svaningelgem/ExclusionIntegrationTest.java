package io.github.svaningelgem;

import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        assertTrue("Generated code should be excluded",
                mojo.isExcluded("com.example.generated.api.UsersApi"));

        assertTrue("Controllers should be excluded",
                mojo.isExcluded("com.example.web.UserController"));

        assertTrue("Model classes should be excluded",
                mojo.isExcluded("com.example.model.User"));

        assertFalse("Normal classes should not be excluded",
                mojo.isExcluded("com.example.Calculator"));
    }

    @Test
    public void testExclusionWithAdditionalPatterns() {
        // Add explicit excludes to the mojo as well
        List<String> additionalExcludes = Arrays.asList(
                "com/example/ignored/**/*",
                "**/*Test.class",
                "com/example/ignored2",
                "com/example/ignored3/"
        );

        // Load the patterns
        mojo.loadExclusionPatterns();
        additionalExcludes.forEach(mojo::addExclusion);

        // Test combined exclusions
        assertTrue("Generated code should be excluded (from JaCoCo config)",
                mojo.isExcluded("com.example.generated.model.User"));

        assertTrue("Ignored classes should be excluded (from additional config)",
                mojo.isExcluded("com.example.ignored.SomeClass"));

        assertTrue("Test classes should be excluded (from additional config)",
                mojo.isExcluded("com.example.service.UserServiceTest"));

        assertTrue("Ignored classes should be excluded (from additional config)",
                mojo.isExcluded("com.example.ignored2.SomeClass"));

        assertTrue("Ignored classes should be excluded (from additional config)",
                mojo.isExcluded("com.example.ignored3.SomeClass"));

    }

    @Test
    public void testAddSwaggerExclusions() {
        mojo.project.getBuild().getPlugins().clear();

        Plugin swaggerCodegenPlugin = createPlugin("io.swagger", "swagger-codegen-maven-plugin", "<output>swagger-output</output><outputDirectory>swagger-outputDirectory</outputDirectory><sth>else1</sth>");
        mojo.project.getBuild().addPlugin(swaggerCodegenPlugin);

        Plugin springdocPlugin = createPlugin("org.springdoc", "springdoc-openapi-maven-plugin", "<outputDir>springdoc-outputDir</outputDir><sth>else2</sth>");
        mojo.project.getBuild().addPlugin(springdocPlugin);

        Plugin openapiPlugin = createPlugin("org.openapitools", "openapi-generator-maven-plugin", "<outputDir>openapi-outputDir</outputDir><output>openapi-output</output><sth>else3</sth>");
        mojo.project.getBuild().addPlugin(openapiPlugin);

        // Execute the method being tested
        mojo.addSwaggerExclusions();

        // We have to call ".pattern" here because one Pattern != another Pattern. Even though their strings are the same :-(
        Set<String> expected = Stream.of("swagger-output", "swagger-outputDirectory", "springdoc-outputDir", "openapi-outputDir", "openapi-output").map(mojo::convertExclusionToPattern).map(Pattern::pattern).collect(Collectors.toSet());
        Set<String> provided = JacocoConsoleReporterMojo.collectedExcludePatterns.stream().map(Pattern::pattern).collect(Collectors.toSet());

        // Verify that the expected patterns were added (3 plugins = 3 patterns)
        assertEquals(provided, expected);

        // Test that specific paths would be excluded
        assertTrue("Swagger generated code should be excluded",
                mojo.isExcluded("path.to.swagger.output.SomeGeneratedClass"));

        assertTrue("SpringDoc generated code should be excluded",
                mojo.isExcluded("path.to.springdoc.output.SomeGeneratedClass"));

        assertTrue("OpenAPI generated code should be excluded",
                mojo.isExcluded("path.to.openapi.output.SomeGeneratedClass"));
    }

    @Test
    public void testMergedExclusionPatterns() {
        mojo.loadExclusionPatterns();

        assertEquals("Should have combined all exclusion patterns", 4, JacocoConsoleReporterMojo.collectedExcludePatterns.size());
    }

    @Test
    public void testAddBuildDirExclusionEnabled() {
        assertEquals(0, JacocoConsoleReporterMojo.collectedExcludePatterns.size());
        mojo.ignoreFilesInBuildDirectory = true;
        mojo.addBuildDirExclusion();
        assertEquals(1, JacocoConsoleReporterMojo.collectedExcludePatterns.size());
    }

    @Test
    public void testAddBuildDirExclusionDisabled() {
        assertEquals(0, JacocoConsoleReporterMojo.collectedExcludePatterns.size());
        mojo.ignoreFilesInBuildDirectory = false;
        mojo.addBuildDirExclusion();
        assertEquals(0, JacocoConsoleReporterMojo.collectedExcludePatterns.size());
    }
}