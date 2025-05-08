package io.github.svaningelgem;

import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jacoco.core.data.ExecutionDataStore;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Method loadExclusionPatterns = JacocoConsoleReporterMojo.class.getDeclaredMethod("loadExclusionPatterns");
        loadExclusionPatterns.setAccessible(true);
        loadExclusionPatterns.invoke(mojo);

        // Get the isExcluded method to test our patterns
        Method isExcluded = JacocoConsoleReporterMojo.class.getDeclaredMethod("isExcluded", String.class);
        isExcluded.setAccessible(true);

        // Verify exclusion patterns are working
        assertTrue("Generated code should be excluded",
                (Boolean) isExcluded.invoke(mojo, "com.example.generated.api.UsersApi"));

        assertTrue("Controllers should be excluded",
                (Boolean) isExcluded.invoke(mojo, "com.example.web.UserController"));

        assertTrue("Model classes should be excluded",
                (Boolean) isExcluded.invoke(mojo, "com.example.model.User"));

        assertFalse("Normal classes should not be excluded",
                (Boolean) isExcluded.invoke(mojo, "com.example.Calculator"));
    }

    @Test
    public void testExclusionWithAdditionalPatterns() throws Exception {
        // Add explicit excludes to the mojo as well
        List<String> additionalExcludes = Arrays.asList(
                "com/example/ignored/**/*",
                "**/*Test.class"
        );

        // Load the patterns
        Method loadExclusionPatterns = JacocoConsoleReporterMojo.class.getDeclaredMethod("loadExclusionPatterns");
        loadExclusionPatterns.setAccessible(true);
        loadExclusionPatterns.invoke(mojo);

        // Get the isExcluded method
        Method isExcluded = JacocoConsoleReporterMojo.class.getDeclaredMethod("isExcluded", String.class);
        isExcluded.setAccessible(true);

        // Test combined exclusions
        assertTrue("Generated code should be excluded (from JaCoCo config)",
                (Boolean) isExcluded.invoke(mojo, "com.example.generated.model.User"));

        assertTrue("Ignored classes should be excluded (from additional config)",
                (Boolean) isExcluded.invoke(mojo, "com.example.ignored.SomeClass"));

        assertTrue("Test classes should be excluded (from additional config)",
                (Boolean) isExcluded.invoke(mojo, "com.example.service.UserServiceTest"));
    }

    @Test
    public void testMergedExclusionPatterns() throws Exception {
        // Load patterns
        Method loadExclusionPatterns = JacocoConsoleReporterMojo.class.getDeclaredMethod("loadExclusionPatterns");
        loadExclusionPatterns.setAccessible(true);
        loadExclusionPatterns.invoke(mojo);

        // Get the patterns list to check its size
        Field patternsField = JacocoConsoleReporterMojo.class.getDeclaredField("excludePatterns");
        patternsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Object> patterns = (HashSet<Object>) patternsField.get(mojo);

        // Should have 4 patterns (3 from JaCoCo config + 1 from additional config)
        assertEquals("Should have combined all exclusion patterns", 4, patterns.size());
    }
}