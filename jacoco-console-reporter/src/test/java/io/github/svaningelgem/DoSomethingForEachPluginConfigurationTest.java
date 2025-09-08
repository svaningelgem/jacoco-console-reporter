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
    public void setUp() throws Exception {
        super.setUp();

        mockProject = mock(MavenProject.class);
        when(mockProject.getBuild()).thenReturn(mojo.project.getBuild());
        mojo.project = mockProject;

        values = new ArrayList<>();
        consumer = values::add;
    }

    @Before
    public void resetValues() {
        values.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPluginNotFound() {
        doReturn(Collections.emptyList()).when(mockProject).getBuildPlugins();
        consumer = mock(Consumer.class);

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer, null);

        verify(consumer, never()).accept(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConfigNotFound() {
        createPlugin("");
        consumer = mock(Consumer.class);

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer, null);

        verify(consumer, never()).accept(anyString());
    }

    @Test
    public void testSimpleConfig() {
        createPlugin("<config>value</config>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer, null);

        assertEquals(1, values.size());
        assertEquals("value", values.get(0));
    }

    @Test
    public void testNestedConfig() {
        createPlugin("<parent><child>childValue</child></parent>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "parent.child", consumer, null);

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

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "root.level1.level2.level3", consumer, null);

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

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "root.excludes.exclude", consumer, null);

        assertEquals(3, values.size());
        assertEquals("pattern1", values.get(0));
        assertEquals("pattern2", values.get(1));
        assertEquals("pattern3", values.get(2));
    }

    @Test
    public void testIncompletePathNoMatch() {
        createPlugin("<root><level1></level1></root>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "root.level1.nonexistent", consumer, null);

        assertTrue(values.isEmpty());
    }

    @Test
    public void testEmptyValues() {
        createPlugin("<config></config>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer, null);

        assertTrue(values.isEmpty());
    }

    @Test
    public void testWhitespaceValues() {
        createPlugin("<config>   </config>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer, null);

        assertTrue(values.isEmpty());
    }

    @Test
    public void testMultiplePlugins() {
        Plugin plugin1 = createPlugin("<config>value1</config>");
        Plugin plugin2 = createPlugin("<config>value2</config>");

        doReturn(Arrays.asList(plugin1, plugin2)).when(mockProject).getBuildPlugins();

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "config", consumer, null);

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

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "root.output", consumer, null);
        assertEquals(1, values.size());
        assertEquals("output-dir", values.get(0));
        values.clear();

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "root.outputDirectory", consumer, null);
        assertEquals(1, values.size());
        assertEquals("output-directory", values.get(0));
    }

    @Test
    public void testWithIterable_PartialMatches() {
        createPlugin("<root>" +
                "<output>output-dir</output>" +
                "<!-- outputDirectory is missing -->" +
                "</root>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "root.output", consumer, null);
        assertEquals(1, values.size());
        assertEquals("output-dir", values.get(0));
        values.clear();

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "root.outputDirectory", consumer, null);
        assertEquals(0, values.size());
    }

    @Test
    public void testWithIterable_NoMatches() {
        createPlugin("<root><unrelated>value</unrelated></root>");


        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "root.output", consumer, null);
        assertEquals(0, values.size());

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT, "root.outputDirectory", consumer, null);
        assertEquals(0, values.size());
    }

    @Test
    public void testUnfoundNode() {
        createPlugin("<a><c>value</c></a>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT,
                "a.b.c", consumer, null);

        assertTrue(values.isEmpty());  // Should find no matches
    }

    @Test
    public void testUnfoundNodeButWithDefault() {
        createPlugin("<a><c>value</c></a>");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT,
                "a.b.c", consumer, "bumba");

        assertEquals(1, values.size());
        assertEquals("bumba", values.get(0));
    }

    @Test
    public void testDontFindAnything() {
        createPlugin("");

        mojo.doSomethingForEachPluginConfiguration(TEST_GROUP, TEST_ARTIFACT,
                "", consumer, null);

        assertTrue(values.isEmpty());  // Should find no matches
    }

    @Test
    public void testSomeOtherPlugin() {
        final String OTHER_GROUP = "org.example";
        final String OTHER_ARTIFACT = "plugin";

        Plugin plugin1 = createPlugin(TEST_GROUP, TEST_ARTIFACT, "<output>output1</output>");
        Plugin plugin2 = createPlugin(TEST_GROUP, OTHER_ARTIFACT, "<output>output2</output>");
        Plugin plugin3 = createPlugin(OTHER_GROUP, TEST_ARTIFACT, "<output>output3</output>");
        Plugin plugin4 = createPlugin(OTHER_GROUP, OTHER_ARTIFACT, "<output>output4</output>");
        Plugin plugin5 = createPlugin(OTHER_GROUP + 1, OTHER_ARTIFACT + 1, "<output>output5</output>");
        doReturn(Arrays.asList(plugin1, plugin2, plugin3, plugin4, plugin5)).when(mockProject).getBuildPlugins();

        mojo.doSomethingForEachPluginConfiguration(OTHER_GROUP, OTHER_ARTIFACT, "output", consumer, null);

        assertEquals(1, values.size());
        assertEquals("output4", values.get(0));  // Should find only the matching path
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