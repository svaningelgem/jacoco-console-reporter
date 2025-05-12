package io.github.svaningelgem;

import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
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
        assertTrue("Generated code should be excluded",
                (Boolean) mojo.isExcluded("com.example.generated.api.UsersApi"));

        assertTrue("Controllers should be excluded",
                (Boolean) mojo.isExcluded("com.example.web.UserController"));

        assertTrue("Model classes should be excluded",
                (Boolean) mojo.isExcluded("com.example.model.User"));

        assertFalse("Normal classes should not be excluded",
                (Boolean) mojo.isExcluded("com.example.Calculator"));
    }

    @Test
    public void testExclusionWithAdditionalPatterns() throws Exception {
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
                (Boolean) mojo.isExcluded("com.example.generated.model.User"));

        assertTrue("Ignored classes should be excluded (from additional config)",
                (Boolean) mojo.isExcluded("com.example.ignored.SomeClass"));

        assertTrue("Test classes should be excluded (from additional config)",
                (Boolean) mojo.isExcluded("com.example.service.UserServiceTest"));

        assertTrue("Ignored classes should be excluded (from additional config)",
                (Boolean) mojo.isExcluded("com.example.ignored2.SomeClass"));

        assertTrue("Ignored classes should be excluded (from additional config)",
                (Boolean) mojo.isExcluded("com.example.ignored3.SomeClass"));

    }

    @Test
    public void testAddSwaggerExclusions() throws Exception {
        // Clear existing patterns
        JacocoConsoleReporterMojo.collectedExcludePatterns.clear();

        // Create plugins for the different Swagger/OpenAPI generators
        // 1. Swagger Codegen Maven Plugin
        Plugin swaggerCodegenPlugin = new Plugin();
        swaggerCodegenPlugin.setGroupId("io.swagger");
        swaggerCodegenPlugin.setArtifactId("swagger-codegen-maven-plugin");
        Xpp3Dom swaggerConfig = new Xpp3Dom("configuration");
        Xpp3Dom swaggerOutput = new Xpp3Dom("output");
        swaggerOutput.setValue("/path/to/swagger/output");
        swaggerConfig.addChild(swaggerOutput);
        swaggerCodegenPlugin.setConfiguration(swaggerConfig);

        // 2. SpringDoc OpenAPI Maven Plugin
        Plugin springdocPlugin = new Plugin();
        springdocPlugin.setGroupId("org.springdoc");
        springdocPlugin.setArtifactId("springdoc-openapi-maven-plugin");
        Xpp3Dom springdocConfig = new Xpp3Dom("configuration");
        Xpp3Dom springdocOutput = new Xpp3Dom("outputDir");
        springdocOutput.setValue("/path/to/springdoc/output");
        springdocConfig.addChild(springdocOutput);
        springdocPlugin.setConfiguration(springdocConfig);

        // 3. OpenAPI Generator Maven Plugin
        Plugin openapiPlugin = new Plugin();
        openapiPlugin.setGroupId("org.openapitools");
        openapiPlugin.setArtifactId("openapi-generator-maven-plugin");
        Xpp3Dom openapiConfig = new Xpp3Dom("configuration");
        Xpp3Dom openapiOutput = new Xpp3Dom("outputDir");
        openapiOutput.setValue("/path/to/openapi/output");
        openapiConfig.addChild(openapiOutput);
        openapiPlugin.setConfiguration(openapiConfig);

        // Add plugins to the project
        mojo.project.getBuild().getPlugins().clear();
        mojo.project.getBuild().addPlugin(swaggerCodegenPlugin);
        mojo.project.getBuild().addPlugin(springdocPlugin);
        mojo.project.getBuild().addPlugin(openapiPlugin);

        // Execute the method being tested
        mojo.addSwaggerExclusions();

        // Verify that the expected patterns were added (3 plugins = 3 patterns)
        assertEquals("Should have added 3 Swagger exclusion patterns", 3,
                JacocoConsoleReporterMojo.collectedExcludePatterns.size());

        // Test that specific paths would be excluded
        assertTrue("Swagger generated code should be excluded",
                mojo.isExcluded("path.to.swagger.output.SomeGeneratedClass"));

        assertTrue("SpringDoc generated code should be excluded",
                mojo.isExcluded("path.to.springdoc.output.SomeGeneratedClass"));

        assertTrue("OpenAPI generated code should be excluded",
                mojo.isExcluded("path.to.openapi.output.SomeGeneratedClass"));
    }

    @Test
    public void testMergedExclusionPatterns() throws Exception {
        mojo.loadExclusionPatterns();

        assertEquals("Should have combined all exclusion patterns", 4, JacocoConsoleReporterMojo.collectedExcludePatterns.size());
    }

    @Test
    public void testAddBuildDirExclusionEnabled() throws Exception {
        assertEquals(0, JacocoConsoleReporterMojo.collectedExcludePatterns.size());
        mojo.ignoreFilesInBuildDirectory = true;
        mojo.addBuildDirExclusion();
        assertEquals(1, JacocoConsoleReporterMojo.collectedExcludePatterns.size());
    }

    @Test
    public void testAddBuildDirExclusionDisabled() throws Exception {
        assertEquals(0, JacocoConsoleReporterMojo.collectedExcludePatterns.size());
        mojo.ignoreFilesInBuildDirectory = false;
        mojo.addBuildDirExclusion();
        assertEquals(0, JacocoConsoleReporterMojo.collectedExcludePatterns.size());
    }
}