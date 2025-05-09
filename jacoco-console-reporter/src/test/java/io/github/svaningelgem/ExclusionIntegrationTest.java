package io.github.svaningelgem;

import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jacoco.core.data.ExecutionDataStore;
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
        // Set up mock objects
        ExecutionDataStore mockStore = new ExecutionDataStore();

        // Create a fake directory structure for class file analysis
        File classesDir = temporaryFolder.newFolder("classes");
        File generatedDir = new File(classesDir, "com/example/generated");
        generatedDir.mkdirs();

        // Add this to the mojo's class paths
        mojo.classesDirectory = classesDir;
        JacocoConsoleReporterMojo.collectedClassesPaths.clear();
        JacocoConsoleReporterMojo.collectedClassesPaths.add(classesDir);

        // First, load the exclusion patterns
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
                "**/*Test.class"
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
    }

    @Test
    public void testMergedExclusionPatterns() throws Exception {
        mojo.loadExclusionPatterns();

        assertEquals("Should have combined all exclusion patterns", 3, mojo.excludePatterns.size());
    }
}