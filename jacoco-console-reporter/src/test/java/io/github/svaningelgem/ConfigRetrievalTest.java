package io.github.svaningelgem;

import lombok.var;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ConfigRetrievalTest extends BaseTestClass {
    Plugin jacocoPlugin;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        jacocoPlugin = findOrCreateJacocoPlugin(mojo.project);
        var configuration = new Xpp3Dom("configuration");
        jacocoPlugin.setConfiguration(configuration);

        var destFileNode = new Xpp3Dom("destFile");
        destFileNode.setValue("jacoco.exec");
        configuration.addChild(destFileNode);

        var configuration2 = new Xpp3Dom("configuration");
        var destFile2 = new Xpp3Dom("destFile");
        destFile2.setValue("jacoco2.exec");
        configuration2.addChild(destFile2);

        var pe = new PluginExecution();
        pe.setId("report");
        pe.setPhase("verify");
        pe.setGoals(Collections.singletonList("report"));
        pe.setConfiguration(configuration2);
        jacocoPlugin.addExecution(pe);
    }

    @Test
    public void testConfigRetrievalViaExecutionStep() {
        var x = mojo.getConfiguration(jacocoPlugin, new String[]{"destFile"});
        var allValues = x.stream().map(Xpp3Dom::getValue).collect(Collectors.toList());
        assertEquals(1, allValues.size());
        assertEquals("jacoco2.exec", allValues.get(0)); // execution step overrules
    }

    @Test
    public void testConfigRetrievalWithoutExecutionStep() {
        mojo.mojoExecution = null;
        var x = mojo.getConfiguration(jacocoPlugin, new String[]{"destFile"});
        var allValues = x.stream().map(Xpp3Dom::getValue).collect(Collectors.toList());
        assertEquals(1, allValues.size());
        assertEquals("jacoco.exec", allValues.get(0)); // we retrieve the generic configuration value
    }

    @Test
    public void testConfigRetrievalViaGenericConfiguration() {
        jacocoPlugin.setExecutions(null);
        var x = mojo.getConfiguration(jacocoPlugin, new String[]{"destFile"});
        var allValues = x.stream().map(Xpp3Dom::getValue).collect(Collectors.toList());
        assertEquals(1, allValues.size());
        assertEquals("jacoco.exec", allValues.get(0)); // we retrieve the generic configuration value
    }

    @Test
    public void testNullConfig() {
        assertNull(mojo.digIntoConfig(null, new String[]{"destFile"}));
    }

    @Test
    public void testNullParts() {
        assertNull(mojo.digIntoConfig(new Xpp3Dom("dom"), null));
    }

    @Test
    public void testNoParts() {
        assertNull(mojo.digIntoConfig(new Xpp3Dom("dom"), new String[]{}));
    }
}
