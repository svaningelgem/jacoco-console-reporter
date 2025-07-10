package io.github.svaningelgem;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.*;

public class SonarExclusionsTest extends BaseTestClass {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Create a fresh project for testing
        Model model = new Model();
        model.setGroupId("test.group");
        model.setArtifactId("test-artifact");
        model.setVersion("1.0.0");

        MavenProject project = new MavenProject(model);
        mojo.project = project;
        mojo.mavenSession = createRealMavenSession(Collections.singletonList(project));
    }

    @Test
    public void testAddSonarExclusionsWithSonarExclusions() {
        Properties props = new Properties();
        props.setProperty("sonar.exclusions", "src/main/java/com/example/generated/**,**/*Generated.java,**/target/**");
        mojo.project.getModel().setProperties(props);

        int initialSize = JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size();

        mojo.addSonarExclusions();

        // Should have added 3 patterns
        assertEquals(initialSize + 3, JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size());

        // Test the exclusions work correctly with file paths
        assertTrue(mojo.isExcluded("com/example/generated/ApiClient", "src/main/java/com/example/generated/ApiClient.java"));
        assertTrue(mojo.isExcluded("com/example/service/UserGenerated", "src/main/java/com/example/service/UserGenerated.java"));
        assertTrue(mojo.isExcluded("any/target/classes", "any/target/classes.java"));
        assertFalse(mojo.isExcluded("com/example/service/UserService", "src/main/java/com/example/service/UserService.java"));
    }

    @Test
    public void testAddSonarExclusionsWithSonarCoverageExclusions() {
        Properties props = new Properties();
        props.setProperty("sonar.coverage.exclusions", "src/test/java/**,**/*Test.java,**/*DTO.java");
        mojo.project.getModel().setProperties(props);

        int initialSize = JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size();

        mojo.addSonarExclusions();

        // Should have added 3 patterns
        assertEquals(initialSize + 3, JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size());

        // Test the exclusions work correctly
        assertTrue(mojo.isExcluded("com/example/UserTest", "src/test/java/com/example/UserTest.java"));
        assertTrue(mojo.isExcluded("com/example/dto/UserDTO", "src/main/java/com/example/dto/UserDTO.java"));
        assertTrue(mojo.isExcluded("com/example/UserTest", "com/example/UserTest.java"));
        assertFalse(mojo.isExcluded("com/example/service/UserService", "src/main/java/com/example/service/UserService.java"));
    }

    @Test
    public void testAddSonarExclusionsWithBothProperties() {
        Properties props = new Properties();
        props.setProperty("sonar.exclusions", "src/main/java/com/example/generated/**");
        props.setProperty("sonar.coverage.exclusions", "**/*Test.java");
        mojo.project.getModel().setProperties(props);

        int initialSize = JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size();

        mojo.addSonarExclusions();

        // Should have added 2 patterns (one from each property)
        assertEquals(initialSize + 2, JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size());

        // Test both exclusions work
        assertTrue(mojo.isExcluded("com/example/generated/ApiClient", "src/main/java/com/example/generated/ApiClient.java"));
        assertTrue(mojo.isExcluded("com/example/UserTest", "src/main/java/com/example/UserTest.java"));
        assertFalse(mojo.isExcluded("com/example/UserService", "src/main/java/com/example/UserService.java"));
    }

    @Test
    public void testAddSonarExclusionsWithEmptyProperties() {
        Properties props = new Properties();
        props.setProperty("sonar.exclusions", "");
        props.setProperty("sonar.coverage.exclusions", "   ");
        mojo.project.getModel().setProperties(props);

        int initialSize = JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size();

        mojo.addSonarExclusions();

        // Should not have added any patterns
        assertEquals(initialSize, JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size());
    }

    @Test
    public void testAddSonarExclusionsWithNoProperties() {
        // No sonar properties set
        Properties props = new Properties();
        mojo.project.getModel().setProperties(props);

        int initialSize = JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size();

        mojo.addSonarExclusions();

        // Should not have added any patterns
        assertEquals(initialSize, JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size());
    }

    @Test
    public void testSonarExclusionPatternMatching() {
        Properties props = new Properties();
        props.setProperty("sonar.exclusions", "src/main/java/com/example/generated/**");
        mojo.project.getModel().setProperties(props);

        mojo.addSonarExclusions();

        SonarExclusionPattern pattern = JacocoConsoleReporterMojo.collectedSonarExcludePatterns.iterator().next();

        // Test various file path formats
        assertTrue(pattern.matches("src/main/java/com/example/generated/ApiClient.java", mojo.project));
        assertTrue(pattern.matches("src/main/java/com/example/generated/model/User.java", mojo.project));
        assertFalse(pattern.matches("src/main/java/com/example/service/UserService.java", mojo.project));
        assertFalse(pattern.matches("src/test/java/com/example/generated/ApiClientTest.java", mojo.project));
    }

    @Test
    public void testAddSonarFileExclusions() {
        String exclusions = "src/main/java/com/example/generated/**,**/*Generated.java, **/target/**, ";

        int initialSize = JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size();

        mojo.addSonarFileExclusions(exclusions);

        // Should have added 3 patterns (empty pattern should be ignored)
        assertEquals(initialSize + 3, JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size());

        // Test the patterns work
        assertTrue(mojo.isExcluded("com/example/generated/ApiClient", "src/main/java/com/example/generated/ApiClient.java"));
        assertTrue(mojo.isExcluded("com/example/UserGenerated", "src/main/java/com/example/UserGenerated.java"));
        assertTrue(mojo.isExcluded("any/target/classes", "any/target/classes.java"));
    }

    @Test
    public void testAddSonarFileExclusionsWithSinglePattern() {
        String exclusions = "src/main/java/com/example/generated/**";

        int initialSize = JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size();

        mojo.addSonarFileExclusions(exclusions);

        // Should have added 1 pattern
        assertEquals(initialSize + 1, JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size());

        assertTrue(mojo.isExcluded("com/example/generated/ApiClient", "src/main/java/com/example/generated/ApiClient.java"));
        assertFalse(mojo.isExcluded("com/example/service/UserService", "src/main/java/com/example/service/UserService.java"));
    }

    @Test
    public void testAddSonarFileExclusionsWithWhitespaceAndEmptyPatterns() {
        String exclusions = " , , src/main/java/com/example/generated/** , , **/*Test.java, ";

        int initialSize = JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size();

        mojo.addSonarFileExclusions(exclusions);

        // Should have added 2 patterns (empty/whitespace patterns should be ignored)
        assertEquals(initialSize + 2, JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size());

        assertTrue(mojo.isExcluded("com/example/generated/ApiClient", "src/main/java/com/example/generated/ApiClient.java"));
        assertTrue(mojo.isExcluded("com/example/UserTest", "src/main/java/com/example/UserTest.java"));
    }

    @Test
    public void testLoadExclusionPatternsIncludesSonarExclusions() {
        Properties props = new Properties();
        props.setProperty("sonar.exclusions", "src/main/java/com/example/generated/**");
        props.setProperty("sonar.coverage.exclusions", "**/*Test.java");
        mojo.project.getModel().setProperties(props);

        // Clear any existing patterns
        JacocoConsoleReporterMojo.collectedSonarExcludePatterns.clear();

        mojo.loadExclusionPatterns();

        // Should have loaded Sonar exclusions
        assertTrue("Should have some Sonar exclusion patterns",
                JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size() > 0);

        // Test that sonar exclusions are working
        assertTrue(mojo.isExcluded("com/example/generated/ApiClient", "src/main/java/com/example/generated/ApiClient.java"));
        assertTrue(mojo.isExcluded("com/example/UserTest", "src/main/java/com/example/UserTest.java"));
    }

    @Test
    public void testSonarExclusionsIntegrationWithJacocoExclusions() {
        // Set up both JaCoCo and Sonar exclusions
        Properties props = new Properties();
        props.setProperty("sonar.exclusions", "src/main/java/com/example/generated/**");
        mojo.project.getModel().setProperties(props);

        // Create a JaCoCo plugin with exclusions
        mojo.project.getBuild().addPlugin(createPlugin("org.jacoco", "jacoco-maven-plugin",
                "<excludes><exclude>com/example/legacy/**/*</exclude></excludes>"));

        // Clear any existing patterns
        JacocoConsoleReporterMojo.collectedExcludePatterns.clear();
        JacocoConsoleReporterMojo.collectedSonarExcludePatterns.clear();

        mojo.loadExclusionPatterns();

        // Should have patterns from both sources
        assertTrue("Should have JaCoCo exclusion patterns",
                JacocoConsoleReporterMojo.collectedExcludePatterns.size() > 0);
        assertTrue("Should have Sonar exclusion patterns",
                JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size() > 0);

        // Test both types of exclusions work
        assertTrue("JaCoCo exclusion should work", mojo.isExcluded("com/example/legacy/OldClass"));
        assertTrue("Sonar exclusion should work", mojo.isExcluded("com/example/generated/ApiClient", "src/main/java/com/example/generated/ApiClient.java"));
        assertFalse("Normal classes should not be excluded", mojo.isExcluded("com/example/service/UserService", "src/main/java/com/example/service/UserService.java"));
    }

    @Test
    public void testSonarExclusionsLogging() {
        Properties props = new Properties();
        props.setProperty("sonar.exclusions", "src/main/java/com/example/generated/**");
        props.setProperty("sonar.coverage.exclusions", "**/*Test.java");
        mojo.project.getModel().setProperties(props);

        mojo.addSonarExclusions();

        // Check that debug messages were logged for the Sonar exclusions
        String[] expected = {
                "[debug] Added Sonar file exclusion pattern: src/main/java/com/example/generated/**",
                "[debug] Added Sonar file exclusion pattern: **/*Test.java"
        };

        assertLogContains(expected);
    }

    @Test
    public void testRealWorldSonarExclusionPatterns() {
        Properties props = new Properties();
        // Real-world patterns commonly used in Sonar
        props.setProperty("sonar.exclusions",
                "src/main/java/**/generated/**," +
                        "src/main/java/**/*Generated.java," +
                        "src/main/java/**/*Dto.java," +
                        "src/main/java/**/config/ApplicationConfig.java," +
                        "**/target/generated-sources/**," +
                        "**/*.proto");

        props.setProperty("sonar.coverage.exclusions",
                "src/test/java/**," +
                        "**/*Test.java," +
                        "**/*IT.java," +
                        "**/*TestCase.java");

        mojo.project.getModel().setProperties(props);

        int initialSize = JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size();

        mojo.addSonarExclusions();

        // Should have added all patterns (6 from sonar.exclusions + 4 from sonar.coverage.exclusions)
        assertEquals(initialSize + 10, JacocoConsoleReporterMojo.collectedSonarExcludePatterns.size());

        // Test various real-world exclusion scenarios
        assertTrue("Generated classes should be excluded",
                mojo.isExcluded("com/example/api/generated/UserApiGenerated", "src/main/java/com/example/api/generated/UserApiGenerated.java"));
        assertTrue("DTO classes should be excluded",
                mojo.isExcluded("com/example/dto/UserDto", "src/main/java/com/example/dto/UserDto.java"));
        assertTrue("Config classes should be excluded",
                mojo.isExcluded("com/example/config/ApplicationConfig", "src/main/java/com/example/config/ApplicationConfig.java"));
        assertTrue("Test classes should be excluded",
                mojo.isExcluded("com/example/service/UserTest", "src/test/java/com/example/service/UserTest.java"));
        assertTrue("Integration test classes should be excluded",
                mojo.isExcluded("com/example/integration/UserIT", "src/test/java/com/example/integration/UserIT.java"));
        assertTrue("TestCase classes should be excluded",
                mojo.isExcluded("com/example/base/BaseTestCase", "src/test/java/com/example/base/BaseTestCase.java"));

        // Normal service classes should not be excluded
        assertFalse("Normal service classes should not be excluded",
                mojo.isExcluded("com/example/service/UserService", "src/main/java/com/example/service/UserService.java"));
        assertFalse("Normal controller classes should not be excluded",
                mojo.isExcluded("com/example/controller/UserController", "src/main/java/com/example/controller/UserController.java"));
    }

    @Test
    public void testBackwardCompatibilityWithoutFilePaths() {
        // Test that the old isExcluded(className) method still works for JaCoCo patterns
        mojo.project.getBuild().addPlugin(createPlugin("org.jacoco", "jacoco-maven-plugin",
                "<excludes><exclude>com/example/legacy/**/*</exclude></excludes>"));

        mojo.loadExclusionPatterns();

        // Should work without file path (only JaCoCo-style exclusions)
        assertTrue("JaCoCo exclusion should work without file path", mojo.isExcluded("com/example/legacy/OldClass"));
        assertFalse("Normal classes should not be excluded", mojo.isExcluded("com/example/service/UserService"));
    }

    @Test
    public void testSonarPatternCompilation() {
        SonarExclusionPattern pattern1 = new SonarExclusionPattern("src/main/java/com/example/generated/**", mojo.project);
        SonarExclusionPattern pattern2 = new SonarExclusionPattern("**/*Test.java", mojo.project);
        SonarExclusionPattern pattern3 = new SonarExclusionPattern("src/main/java/com/example/config/ApplicationConfig.java", mojo.project);

        // Test wildcard patterns
        assertTrue(pattern1.matches("src/main/java/com/example/generated/ApiClient.java", mojo.project));
        assertTrue(pattern1.matches("src/main/java/com/example/generated/model/User.java", mojo.project));
        assertFalse(pattern1.matches("src/main/java/com/example/service/UserService.java", mojo.project));

        // Test suffix patterns
        assertTrue(pattern2.matches("com/example/UserTest.java", mojo.project));
        assertTrue(pattern2.matches("src/test/java/com/example/UserTest.java", mojo.project));
        assertFalse(pattern2.matches("com/example/UserService.java", mojo.project));

        // Test exact patterns
        assertTrue(pattern3.matches("src/main/java/com/example/config/ApplicationConfig.java", mojo.project));
        assertFalse(pattern3.matches("src/main/java/com/example/config/DatabaseConfig.java", mojo.project));
    }

    @Test
    public void testSonarPatternWithSpecialCharacters() {
        // Test patterns with regex special characters
        SonarExclusionPattern pattern1 = new SonarExclusionPattern("src/main/java/com/example/(generated)/**", mojo.project);
        SonarExclusionPattern pattern2 = new SonarExclusionPattern("**/*$InnerClass.java", mojo.project);
        SonarExclusionPattern pattern3 = new SonarExclusionPattern("**/config/*.properties", mojo.project);

        // Test that special regex characters are properly escaped
        assertTrue("Parentheses should be escaped",
                pattern1.matches("src/main/java/com/example/(generated)/ApiClient.java", mojo.project));
        assertTrue("Dollar signs should be escaped",
                pattern2.matches("src/main/java/com/example/Outer$InnerClass.java", mojo.project));
        assertTrue("Properties files should match",
                pattern3.matches("src/main/java/com/example/config/app.properties", mojo.project));

        // Test that similar but different paths don't match
        assertFalse("Different parentheses should not match",
                pattern1.matches("src/main/java/com/example/generated/ApiClient.java", mojo.project));
        assertFalse("Different inner class should not match",
                pattern2.matches("src/main/java/com/example/OuterInnerClass.java", mojo.project));
        assertFalse("Java files should not match properties pattern",
                pattern3.matches("src/main/java/com/example/config/Config.java", mojo.project));
    }

    @Test
    public void testSonarPatternRelativePaths() {
        // Create two projects to test relative path calculation
        Model model1 = new Model();
        model1.setGroupId("test.group");
        model1.setArtifactId("project1");
        model1.setVersion("1.0.0");
        MavenProject project1 = new MavenProject(model1);

        Model model2 = new Model();
        model2.setGroupId("test.group");
        model2.setArtifactId("project2");
        model2.setVersion("1.0.0");
        MavenProject project2 = new MavenProject(model2);

        // Create a pattern from project1
        SonarExclusionPattern pattern = new SonarExclusionPattern("src/main/java/com/example/**", project1);

        // Test that relative path calculation works correctly
        String relativePath = pattern.getRelativePath("com/example/Test.java", project2);
        assertNotNull("Relative path should not be null", relativePath);

        // Test matching with different projects
        assertTrue("Pattern should match regardless of source project",
                pattern.matches("src/main/java/com/example/ApiClient.java", project1));
        assertTrue("Pattern should match regardless of target project",
                pattern.matches("src/main/java/com/example/ApiClient.java", project2));
    }

    @Test
    public void testWildcardPatternCompilation() {
        // Test **/ pattern (directory wildcard)
        SonarExclusionPattern pattern1 = new SonarExclusionPattern("src/main/java/**/generated/**", mojo.project);
        assertTrue("**/ should match nested directories",
                pattern1.matches("src/main/java/com/example/generated/ApiClient.java", mojo.project));
        assertTrue("**/ should match deeply nested directories",
                pattern1.matches("src/main/java/com/example/deep/nested/generated/ApiClient.java", mojo.project));
        assertTrue("**/ should match direct subdirectory",
                pattern1.matches("src/main/java/generated/ApiClient.java", mojo.project));

        // Test trailing ** pattern (matches everything remaining)
        SonarExclusionPattern pattern2 = new SonarExclusionPattern("src/main/java/com/example/**", mojo.project);
        assertTrue("Trailing ** should match files",
                pattern2.matches("src/main/java/com/example/ApiClient.java", mojo.project));
        assertTrue("Trailing ** should match subdirectories",
                pattern2.matches("src/main/java/com/example/service/UserService.java", mojo.project));
        assertTrue("Trailing ** should match deeply nested",
                pattern2.matches("src/main/java/com/example/very/deep/nested/Class.java", mojo.project));
        assertFalse("Trailing ** should not match different base path",
                pattern2.matches("src/main/java/com/other/ApiClient.java", mojo.project));

        // Test single * pattern (filename wildcard)
        SonarExclusionPattern pattern3 = new SonarExclusionPattern("src/main/java/com/example/*Test.java", mojo.project);
        assertTrue("Single * should match filename",
                pattern3.matches("src/main/java/com/example/UserTest.java", mojo.project));
        assertTrue("Single * should match empty prefix",
                pattern3.matches("src/main/java/com/example/Test.java", mojo.project));
        assertFalse("Single * should not cross directory boundaries",
                pattern3.matches("src/main/java/com/example/service/UserTest.java", mojo.project));

        // Test mixed patterns
        SonarExclusionPattern pattern4 = new SonarExclusionPattern("**/test/**/*Test.java", mojo.project);
        assertTrue("Mixed wildcards should work",
                pattern4.matches("src/test/java/com/example/UserTest.java", mojo.project));
        assertTrue("Mixed wildcards should work with nested",
                pattern4.matches("any/path/test/nested/SomeTest.java", mojo.project));
        assertFalse("Mixed wildcards should not match wrong suffix",
                pattern4.matches("any/path/test/nested/SomeService.java", mojo.project));
    }

    @Test
    public void testWildcardEdgeCases() {
        // Test pattern ending with **/ (should be treated as directory wildcard)
        SonarExclusionPattern pattern1 = new SonarExclusionPattern("src/main/java/generated/**/", mojo.project);
        assertTrue("**/ at end should match directories",
                pattern1.matches("src/main/java/generated/api/", mojo.project));
        assertTrue("**/ at end should match nested directories",
                pattern1.matches("src/main/java/generated/api/model/", mojo.project));

        // Test multiple ** in sequence
        SonarExclusionPattern pattern2 = new SonarExclusionPattern("src/**/generated/**/test/**", mojo.project);
        assertTrue("Multiple ** should work",
                pattern2.matches("src/main/java/generated/api/test/UserTest.java", mojo.project));
        assertTrue("Multiple ** should work with deep nesting",
                pattern2.matches("src/main/java/com/example/generated/model/test/deep/UserTest.java", mojo.project));

        // Test ** at the beginning
        SonarExclusionPattern pattern3 = new SonarExclusionPattern("**/generated/**", mojo.project);
        assertTrue("** at start should match",
                pattern3.matches("any/path/generated/ApiClient.java", mojo.project));
        assertTrue("** at start should match root",
                pattern3.matches("generated/ApiClient.java", mojo.project));

        // Test single ** (should match everything)
        SonarExclusionPattern pattern4 = new SonarExclusionPattern("**", mojo.project);
        assertTrue("Single ** should match everything",
                pattern4.matches("any/file.java", mojo.project));
        assertTrue("Single ** should match nested",
                pattern4.matches("very/deep/nested/file.java", mojo.project));
        assertTrue("Single ** should match root file",
                pattern4.matches("file.java", mojo.project));
    }

    @Test
    public void testRelativePathCalculationWithDifferentProjects() throws Exception {
        // Create temporary directories for testing
        File tempDir = Files.createTempDirectory("maven-test").toFile();
        File sourceProjectDir = new File(tempDir, "source-project");
        File currentProjectDir = new File(tempDir, "current-project");

        sourceProjectDir.mkdirs();
        currentProjectDir.mkdirs();

        try {
            // Create source project with base directory
            Model sourceModel = new Model();
            sourceModel.setGroupId("test.group");
            sourceModel.setArtifactId("source-project");
            sourceModel.setVersion("1.0.0");
            MavenProject sourceProject = new MavenProject(sourceModel);
            sourceProject.setFile(new File(sourceProjectDir, "pom.xml"));

            // Create current project with base directory
            Model currentModel = new Model();
            currentModel.setGroupId("test.group");
            currentModel.setArtifactId("current-project");
            currentModel.setVersion("1.0.0");
            MavenProject currentProject = new MavenProject(currentModel);
            currentProject.setFile(new File(currentProjectDir, "pom.xml"));

            // Create pattern from source project
            SonarExclusionPattern pattern = new SonarExclusionPattern("src/main/java/**", sourceProject);

            // Test relative path calculation
            String relativePath = pattern.getRelativePath("com/example/Test.java", currentProject);

            // The relative path should include the path from source to current project
            assertTrue(relativePath.contains("current-project"));
            assertTrue(relativePath.endsWith("com/example/Test.java"));
            assertFalse(relativePath.contains("\\")); // Should use forward slashes

        } finally {
            // Cleanup
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void testRelativePathCalculationWithNestedProjects() throws Exception {
        File tempDir = Files.createTempDirectory("maven-test").toFile();
        File parentDir = new File(tempDir, "parent");
        File sourceProjectDir = new File(parentDir, "source");
        File currentProjectDir = new File(parentDir, "modules/current");

        parentDir.mkdirs();
        sourceProjectDir.mkdirs();
        currentProjectDir.mkdirs();

        try {
            // Create source project
            Model sourceModel = new Model();
            sourceModel.setGroupId("test.group");
            sourceModel.setArtifactId("source");
            sourceModel.setVersion("1.0.0");
            MavenProject sourceProject = new MavenProject(sourceModel);
            sourceProject.setFile(new File(sourceProjectDir, "pom.xml"));

            // Create current project (nested under modules)
            Model currentModel = new Model();
            currentModel.setGroupId("test.group");
            currentModel.setArtifactId("current");
            currentModel.setVersion("1.0.0");
            MavenProject currentProject = new MavenProject(currentModel);
            currentProject.setFile(new File(currentProjectDir, "pom.xml"));

            SonarExclusionPattern pattern = new SonarExclusionPattern("src/main/java/**", sourceProject);

            String relativePath = pattern.getRelativePath("com/example/Test.java", currentProject);

            // Should navigate from source to current via modules
            assertTrue(relativePath.contains("modules/current"));
            assertTrue(relativePath.endsWith("com/example/Test.java"));

        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void testRelativePathCalculationWithSameProjects() {
        Model model = new Model();
        model.setGroupId("test.group");
        model.setArtifactId("same-project");
        model.setVersion("1.0.0");
        MavenProject project = new MavenProject(model);

        SonarExclusionPattern pattern = new SonarExclusionPattern("src/main/java/**", project);

        // When source and current are the same project, should return original path
        String result = pattern.getRelativePath("com/example/Test.java", project);

        assertEquals("com/example/Test.java", result);
    }

    @Test
    public void testRelativePathCalculationWithNullBaseDirs() {
        // Create projects without setting base directories (basedir will be null)
        Model sourceModel = new Model();
        sourceModel.setGroupId("test.group");
        sourceModel.setArtifactId("source");
        MavenProject sourceProject = new MavenProject(sourceModel);

        Model currentModel = new Model();
        currentModel.setGroupId("test.group");
        currentModel.setArtifactId("current");
        MavenProject currentProject = new MavenProject(currentModel);

        SonarExclusionPattern pattern = new SonarExclusionPattern("src/main/java/**", sourceProject);

        // Should fall back to original file path when base directories are null
        String result = pattern.getRelativePath("com/example/Test.java", currentProject);

        assertEquals("com/example/Test.java", result);
    }

    @Test
    public void testRelativePathCalculationWithSourceBaseDirNull() throws Exception {
        File tempDir = Files.createTempDirectory("maven-test").toFile();
        File currentProjectDir = new File(tempDir, "current");
        currentProjectDir.mkdirs();

        try {
            // Source project without base directory
            Model sourceModel = new Model();
            sourceModel.setGroupId("test.group");
            sourceModel.setArtifactId("source");
            MavenProject sourceProject = new MavenProject(sourceModel);

            // Current project with base directory
            Model currentModel = new Model();
            currentModel.setGroupId("test.group");
            currentModel.setArtifactId("current");
            MavenProject currentProject = new MavenProject(currentModel);
            currentProject.setFile(new File(currentProjectDir, "pom.xml"));

            SonarExclusionPattern pattern = new SonarExclusionPattern("src/main/java/**", sourceProject);

            // Should fall back to original path when source base dir is null
            String result = pattern.getRelativePath("com/example/Test.java", currentProject);

            assertEquals("com/example/Test.java", result);

        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void testRelativePathCalculationWithCurrentBaseDirNull() throws Exception {
        File tempDir = Files.createTempDirectory("maven-test").toFile();
        File sourceProjectDir = new File(tempDir, "source");
        sourceProjectDir.mkdirs();

        try {
            // Source project with base directory
            Model sourceModel = new Model();
            sourceModel.setGroupId("test.group");
            sourceModel.setArtifactId("source");
            MavenProject sourceProject = new MavenProject(sourceModel);
            sourceProject.setFile(new File(sourceProjectDir, "pom.xml"));

            // Current project without base directory
            Model currentModel = new Model();
            currentModel.setGroupId("test.group");
            currentModel.setArtifactId("current");
            MavenProject currentProject = new MavenProject(currentModel);

            SonarExclusionPattern pattern = new SonarExclusionPattern("src/main/java/**", sourceProject);

            // Should fall back to original path when current base dir is null
            String result = pattern.getRelativePath("com/example/Test.java", currentProject);

            assertEquals("com/example/Test.java", result);

        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void testRelativePathCalculationWithPathRelativisationException() throws Exception {
        File tempDir = Files.createTempDirectory("maven-test").toFile();
        File sourceProjectDir = new File(tempDir, "source");
        File currentProjectDir = new File("/tmp/completely-different-root"); // Different filesystem root

        sourceProjectDir.mkdirs();

        try {
            Model sourceModel = new Model();
            sourceModel.setGroupId("test.group");
            sourceModel.setArtifactId("source");
            MavenProject sourceProject = new MavenProject(sourceModel);
            sourceProject.setFile(new File(sourceProjectDir, "pom.xml"));

            Model currentModel = new Model();
            currentModel.setGroupId("test.group");
            currentModel.setArtifactId("current");
            MavenProject currentProject = new MavenProject(currentModel);
            currentProject.setFile(new File(currentProjectDir, "pom.xml"));

            SonarExclusionPattern pattern = new SonarExclusionPattern("src/main/java/**", sourceProject);

            // Should fall back to original path when relativization fails
            String result = pattern.getRelativePath("com/example/Test.java", currentProject);

            assertEquals("com/example/Test.java", result);

        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void testRelativePathCalculationWithWindowsStylePaths() throws Exception {
        File tempDir = Files.createTempDirectory("maven-test").toFile();
        File sourceProjectDir = new File(tempDir, "source");
        File currentProjectDir = new File(tempDir, "modules\\current"); // Windows-style path

        sourceProjectDir.mkdirs();
        currentProjectDir.mkdirs();

        try {
            Model sourceModel = new Model();
            sourceModel.setGroupId("test.group");
            sourceModel.setArtifactId("source");
            MavenProject sourceProject = new MavenProject(sourceModel);
            sourceProject.setFile(new File(sourceProjectDir, "pom.xml"));

            Model currentModel = new Model();
            currentModel.setGroupId("test.group");
            currentModel.setArtifactId("current");
            MavenProject currentProject = new MavenProject(currentModel);
            currentProject.setFile(new File(currentProjectDir, "pom.xml"));

            SonarExclusionPattern pattern = new SonarExclusionPattern("src/main/java/**", sourceProject);

            String result = pattern.getRelativePath("com\\example\\Test.java", currentProject);

            // Should convert backslashes to forward slashes
            assertFalse(result.contains("\\"));
            assertTrue(result.contains("/"));
            assertTrue(result.endsWith("com/example/Test.java"));

        } finally {
            deleteDirectory(tempDir);
        }
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}