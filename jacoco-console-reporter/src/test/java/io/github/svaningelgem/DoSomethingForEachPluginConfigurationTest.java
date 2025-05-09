package io.github.svaningelgem;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class DoSomethingForEachPluginConfigurationTest extends BaseTestClass {

    public static final String TEST_GROUP = "test.group";
    public static final String TEST_ARTIFACT = "test.artifact";
    @Mock
    private MavenProject mockProject;

    @Override
    @Before
    public void setUp() throws Exception {
        // Don't call super.setUp() since we're using mocks for this test
        // instead of the real implementation from BaseTestClass
        mojo = new JacocoConsoleReporterMojo();
        mockProject = mock(MavenProject.class);
        mojo.project = mockProject;

        // Initialize a log for the mojo
        log = new MyLog();
        mojo.setLog(log);
    }

    @Test
    public void testPluginNotFound() {
        // Arrange
        doReturn(Collections.emptyList()).when(mockProject).getBuildPlugins();
        Consumer<String> consumer = mock(Consumer.class);

        // Act
        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer);

        // Assert
        verify(consumer, never()).accept(anyString());
    }

    @Test
    public void testConfigNotFound() {
        // Arrange
        Plugin plugin = createPlugin(null);
        doReturn(Collections.singletonList(plugin)).when(mockProject).getBuildPlugins();
        Consumer<String> consumer = mock(Consumer.class);

        // Act
        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer);

        // Assert
        verify(consumer, never()).accept(anyString());
    }

    @Test
    public void testSimpleConfig() {
        // Arrange
        Xpp3Dom config = createConfig("config", "value");
        Plugin plugin = createPlugin(config);
        doReturn(Collections.singletonList(plugin)).when(mockProject).getBuildPlugins();

        List<String> values = new ArrayList<>();
        Consumer<String> consumer = values::add;

        // Act
        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer);

        // Assert
        assertEquals(1, values.size());
        assertEquals("value", values.get(0));
    }

    @Test
    public void testNestedConfig() {
        // Arrange
        Xpp3Dom parent = new Xpp3Dom("parent");
        Xpp3Dom child = createConfig("child", "childValue");
        parent.addChild(child);

        Plugin plugin = createPlugin(parent);
        doReturn(Collections.singletonList(plugin)).when(mockProject).getBuildPlugins();

        List<String> values = new ArrayList<>();
        Consumer<String> consumer = values::add;

        // Act
        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "parent.child", consumer);

        // Assert
        assertEquals(1, values.size());
        assertEquals("childValue", values.get(0));
    }

    @Test
    public void testDeepNestedConfig() {
        // Arrange
        Xpp3Dom root = new Xpp3Dom("root");
        Xpp3Dom level1 = new Xpp3Dom("level1");
        Xpp3Dom level2 = new Xpp3Dom("level2");
        Xpp3Dom level3 = createConfig("level3", "deepValue");

        level2.addChild(level3);
        level1.addChild(level2);
        root.addChild(level1);

        Plugin plugin = createPlugin(root);
        doReturn(Collections.singletonList(plugin)).when(mockProject).getBuildPlugins();

        List<String> values = new ArrayList<>();
        Consumer<String> consumer = values::add;

        // Act
        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "root.level1.level2.level3", consumer);

        // Assert
        assertEquals(1, values.size());
        assertEquals("deepValue", values.get(0));
    }

    @Test
    public void testMultipleMatchingNodes() {
        // Arrange
        Xpp3Dom root = new Xpp3Dom("root");

        Xpp3Dom excludes = new Xpp3Dom("excludes");
        root.addChild(excludes);

        Xpp3Dom exclude1 = createConfig("exclude", "pattern1");
        Xpp3Dom exclude2 = createConfig("exclude", "pattern2");
        Xpp3Dom exclude3 = createConfig("exclude", "pattern3");

        excludes.addChild(exclude1);
        excludes.addChild(exclude2);
        excludes.addChild(exclude3);

        Plugin plugin = createPlugin(root);
        doReturn(Collections.singletonList(plugin)).when(mockProject).getBuildPlugins();

        List<String> values = new ArrayList<>();
        Consumer<String> consumer = values::add;

        // Act
        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "excludes.exclude", consumer);

        // Assert
        assertEquals(3, values.size());
        assertEquals("pattern1", values.get(0));
        assertEquals("pattern2", values.get(1));
        assertEquals("pattern3", values.get(2));
    }

    @Test
    public void testIncompletePathNoMatch() {
        // Arrange
        Xpp3Dom root = new Xpp3Dom("root");
        Xpp3Dom level1 = new Xpp3Dom("level1");
        root.addChild(level1);

        Plugin plugin = createPlugin(root);
        doReturn(Collections.singletonList(plugin)).when(mockProject).getBuildPlugins();

        List<String> values = new ArrayList<>();
        Consumer<String> consumer = values::add;

        // Act
        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "root.level1.nonexistent", consumer);

        // Assert
        assertTrue(values.isEmpty());
    }

    @Test
    public void testEmptyValues() {
        // Arrange
        Xpp3Dom config = new Xpp3Dom("config");
        config.setValue("");  // Empty value

        Plugin plugin = createPlugin(config);
        doReturn(Collections.singletonList(plugin)).when(mockProject).getBuildPlugins();

        List<String> values = new ArrayList<>();
        Consumer<String> consumer = values::add;

        // Act
        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer);

        // Assert
        assertTrue(values.isEmpty());  // Should ignore empty values
    }

    @Test
    public void testWhitespaceValues() {
        // Arrange
        Xpp3Dom config = new Xpp3Dom("config");
        config.setValue("   ");  // Whitespace-only value

        Plugin plugin = createPlugin(config);
        doReturn(Collections.singletonList(plugin)).when(mockProject).getBuildPlugins();

        List<String> values = new ArrayList<>();
        Consumer<String> consumer = values::add;

        // Act
        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer);

        // Assert
        assertTrue(values.isEmpty());  // Should ignore whitespace-only values
    }

    @Test
    public void testMultiplePlugins() {
        // Arrange
        Xpp3Dom config1 = createConfig("config", "value1");
        Plugin plugin1 = createPlugin(config1);

        Xpp3Dom config2 = createConfig("config", "value2");
        Plugin plugin2 = createPlugin(config2);

        doReturn(Arrays.asList(plugin1, plugin2)).when(mockProject).getBuildPlugins();

        List<String> values = new ArrayList<>();
        Consumer<String> consumer = values::add;

        // Act
        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer);

        // Assert
        assertEquals(2, values.size());
        assertEquals("value1", values.get(0));
        assertEquals("value2", values.get(1));
    }

    @Test
    public void testWithIterable_ProcessesAllValues() {
        // Arrange
        Xpp3Dom root = new Xpp3Dom("root");
        Xpp3Dom output = createConfig("output", "output-dir");
        Xpp3Dom outputDir = createConfig("outputDirectory", "output-directory");
        root.addChild(output);
        root.addChild(outputDir);

        Plugin plugin = createPlugin(root);
        doReturn(Collections.singletonList(plugin)).when(mockProject).getBuildPlugins();

        List<String> values = new ArrayList<>();
        Consumer<String> consumer = values::add;

        // Act
        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT,
                Arrays.asList("output", "outputDirectory"), consumer);

        // Assert
        assertEquals(2, values.size());
        assertEquals("output-dir", values.get(0));
        assertEquals("output-directory", values.get(1));
    }

    @Test
    public void testWithIterable_PartialMatches() {
        // Arrange
        Xpp3Dom root = new Xpp3Dom("root");
        Xpp3Dom output = createConfig("output", "output-dir");
        root.addChild(output);
        // Note: outputDirectory is missing

        Plugin plugin = createPlugin(root);
        doReturn(Collections.singletonList(plugin)).when(mockProject).getBuildPlugins();

        List<String> values = new ArrayList<>();
        Consumer<String> consumer = values::add;

        // Act
        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT,
                Arrays.asList("output", "outputDirectory"), consumer);

        // Assert
        assertEquals(1, values.size());
        assertEquals("output-dir", values.get(0));  // Should find only the matching path
    }

    @Test
    public void testWithIterable_NoMatches() {
        // Arrange
        Xpp3Dom root = new Xpp3Dom("root");
        Xpp3Dom unrelated = createConfig("unrelated", "value");
        root.addChild(unrelated);

        Plugin plugin = createPlugin(root);
        doReturn(Collections.singletonList(plugin)).when(mockProject).getBuildPlugins();

        List<String> values = new ArrayList<>();
        Consumer<String> consumer = values::add;

        // Act
        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT,
                Arrays.asList("output", "outputDirectory"), consumer);

        // Assert
        assertTrue(values.isEmpty());  // Should find no matches
    }

    // Helper methods specific to this test
    private @NotNull Plugin createPlugin(Xpp3Dom configuration) {
        Plugin plugin = new Plugin();
        plugin.setGroupId(TEST_GROUP);
        plugin.setArtifactId(TEST_ARTIFACT);
        plugin.setConfiguration(configuration);
        return plugin;
    }

    private @NotNull Xpp3Dom createConfig(String name, String value) {
        Xpp3Dom config = new Xpp3Dom(name);
        config.setValue(value);
        return config;
    }
}