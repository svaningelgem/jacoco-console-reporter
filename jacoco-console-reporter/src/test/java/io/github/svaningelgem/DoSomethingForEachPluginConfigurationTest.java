package io.github.svaningelgem;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @Mock
    private MavenProject mockProject;

    private List<String> values;
    private Consumer<String> consumer;

    @Override
    @Before
    public void setUp() {
        mojo = new JacocoConsoleReporterMojo();
        mockProject = mock(MavenProject.class);
        mojo.project = mockProject;

        log = new MyLog();
        mojo.setLog(log);

        values = new ArrayList<>();
        consumer = values::add;
    }

    @Before
    public void resetValues() {
        values.clear();
    }

    @Test
    public void testPluginNotFound() {
        doReturn(Collections.emptyList()).when(mockProject).getBuildPlugins();
        consumer = mock(Consumer.class);

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer);

        verify(consumer, never()).accept(anyString());
    }

    @Test
    public void testConfigNotFound() {
        createPlugin("");
        consumer = mock(Consumer.class);

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer);

        verify(consumer, never()).accept(anyString());
    }

    @Test
    public void testSimpleConfig() {
        createPlugin("<config>value</config>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer);

        assertEquals(1, values.size());
        assertEquals("value", values.get(0));
    }

    @Test
    public void testNestedConfig() {
        createPlugin("<parent><child>childValue</child></parent>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "parent.child", consumer);

        assertEquals(1, values.size());
        assertEquals("childValue", values.get(0));
    }

    @Test
    public void testDeepNestedConfig() {
        createPlugin(
                "<root>" +
                        "<level1>" +
                        "<level2>" +
                        "<level3>deepValue</level3>" +
                        "</level2>" +
                        "</level1>" +
                        "</root>"
        );

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "root.level1.level2.level3", consumer);

        assertEquals(1, values.size());
        assertEquals("deepValue", values.get(0));
    }

    @Test
    public void testMultipleMatchingNodes() {
        createPlugin(
                "<root>" +
                        "<excludes>" +
                        "<exclude>pattern1</exclude>" +
                        "<exclude>pattern2</exclude>" +
                        "<exclude>pattern3</exclude>" +
                        "</excludes>" +
                        "</root>"
        );

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "root.excludes.exclude", consumer);

        assertEquals(3, values.size());
        assertEquals("pattern1", values.get(0));
        assertEquals("pattern2", values.get(1));
        assertEquals("pattern3", values.get(2));
    }

    @Test
    public void testIncompletePathNoMatch() {
        createPlugin("<root><level1></level1></root>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "root.level1.nonexistent", consumer);

        assertTrue(values.isEmpty());
    }

    @Test
    public void testEmptyValues() {
        createPlugin("<config></config>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer);

        assertTrue(values.isEmpty());
    }

    @Test
    public void testWhitespaceValues() {
        createPlugin("<config>   </config>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer);

        assertTrue(values.isEmpty());
    }

    @Test
    public void testMultiplePlugins() {
        Plugin plugin1 = createPlugin("<config>value1</config>");
        Plugin plugin2 = createPlugin("<config>value2</config>");

        doReturn(Arrays.asList(plugin1, plugin2)).when(mockProject).getBuildPlugins();

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer);

        assertEquals(2, values.size());
        assertEquals("value1", values.get(0));
        assertEquals("value2", values.get(1));
    }

    @Test
    public void testWithIterable_ProcessesAllValues() {
        createPlugin("<root>" +
                "<output>output-dir</output>" +
                "<outputDirectory>output-directory</outputDirectory>" +
                "</root>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT,
                Arrays.asList("root.output", "root.outputDirectory"), consumer);

        assertEquals(2, values.size());
        assertEquals("output-dir", values.get(0));
        assertEquals("output-directory", values.get(1));
    }

    @Test
    public void testWithIterable_PartialMatches() {
        createPlugin("<root>" +
                "<output>output-dir</output>" +
                "<!-- outputDirectory is missing -->" +
                "</root>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT,
                Arrays.asList("root.output", "root.outputDirectory"), consumer);

        assertEquals(1, values.size());
        assertEquals("output-dir", values.get(0));  // Should find only the matching path
    }

    @Test
    public void testWithIterable_NoMatches() {
        createPlugin("<root><unrelated>value</unrelated></root>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT,
                Arrays.asList("root.output", "root.outputDirectory"), consumer);

        assertTrue(values.isEmpty());  // Should find no matches
    }

    @Test
    public void testUnfoundNode() {
        createPlugin("<a><c>value</c></a>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT,
                "a.b.c", consumer);

        assertTrue(values.isEmpty());  // Should find no matches
    }

    @Test
    public void testDontFindAnything() {
        createPlugin("");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT,
                "", consumer);

        assertTrue(values.isEmpty());  // Should find no matches
    }

    @Test
    public void testSomeOtherPlugin() {
        Plugin plugin = createPlugin("some.example", "plugin", "<output>output</output>");
        doReturn(Collections.singletonList(plugin)).when(mockProject).getBuildPlugins();

        mojo.doSomethingForEachPluginConfiguration("some.example", "plugin",
                "output", consumer);

        assertEquals(1, values.size());
        assertEquals("output", values.get(0));  // Should find only the matching path
    }

    @Override
    protected @NotNull Plugin createPlugin(@Nullable Xpp3Dom configuration) {
        Plugin plugin = super.createPlugin(configuration);
        doReturn(Collections.singletonList(plugin)).when(mockProject).getBuildPlugins();
        return plugin;
    }

    @Override
    protected @NotNull Plugin createPlugin(@Nullable String xml) {
        Plugin plugin = super.createPlugin(xml);
        doReturn(Collections.singletonList(plugin)).when(mockProject).getBuildPlugins();
        return plugin;
    }
}