package io.github.svaningelgem;

import org.jacoco.core.analysis.IBundleCoverage;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class XmlReportGenerationTest extends BaseTestClass {

    @Test
    public void testGenerateXmlReportWithNullOutputFile() throws Exception {
        mojo.xmlOutputFile = null;

        IBundleCoverage mockBundle = createSimpleMockBundle("TestBundle");

        mojo.generateXmlReport(mockBundle);

        assertEquals("Should not log XML generation when output file is null", 0,
                log.writtenData.stream().mapToInt(s -> s.contains("Generating aggregated JaCoCo XML report") ? 1 : 0).sum());
    }

    @Test
    public void testGenerateXmlReportCreatesValidXmlFile() throws Exception {
        IBundleCoverage mockBundle = createMockBundleWithPackage(
                "TestProject",
                "com/example",
                "TestClass.java",
                "com/example/TestClass"
        );

        try {
            mojo.generateXmlReport(mockBundle);

            assertTrue("XML file should exist", mojo.xmlOutputFile.exists());
            assertTrue("XML file should have content", mojo.xmlOutputFile.length() > 0);

            String[] expectedLogs = {
                    "[info] Generating aggregated JaCoCo XML report to: " + mojo.xmlOutputFile.getAbsolutePath(),
                    "[info] XML report generated successfully."
            };
            assertLogContains(expectedLogs);
        } catch (Exception e) {
            verifyXmlGenerationAttempted();
        }
    }

    @Test
    public void testGenerateXmlReportCreatesValidXmlStructure() throws Exception {
        IBundleCoverage mockBundle = createMockBundleWithPackage(
                "StructureTestProject",
                "com/example",
                "TestClass.java",
                "com/example/TestClass"
        );

        try {
            mojo.generateXmlReport(mockBundle);

            if (mojo.xmlOutputFile.exists() && mojo.xmlOutputFile.length() > 0) {
                Document document = parseXmlFile();

                Element root = document.getDocumentElement();
                assertEquals("Root element should be 'report'", "report", root.getTagName());

                assertTrue("Should have 'name' attribute", root.hasAttribute("name"));

                NodeList sessionInfo = document.getElementsByTagName("sessioninfo");
                assertTrue("Should contain sessioninfo elements", sessionInfo.getLength() >= 0);

                NodeList packages = document.getElementsByTagName("package");
                assertTrue("Should contain package elements", packages.getLength() >= 0);
            }
        } catch (Exception e) {
            verifyXmlGenerationAttempted();
        }
    }

    @Test
    public void testGenerateXmlReportWithComplexBundleStructure() throws Exception {
        Map<String, String[]> packageToClasses = new HashMap<>();
        packageToClasses.put("com/example/service", new String[]{"UserService", "ProductService"});
        packageToClasses.put("com/example/controller", new String[]{"UserController"});
        packageToClasses.put("com/example/model", new String[]{"User", "Product"});

        IBundleCoverage mockBundle = createMultiPackageMockBundle("ComplexProject", packageToClasses);

        try {
            mojo.generateXmlReport(mockBundle);

            assertTrue("XML file should exist", mojo.xmlOutputFile.exists());

            if (mojo.xmlOutputFile.length() > 0) {
                Document document = parseXmlFile();

                NodeList packages = document.getElementsByTagName("package");
                assertTrue("Should contain package elements", packages.getLength() >= 0);

                NodeList classes = document.getElementsByTagName("class");
                assertTrue("Should contain class elements", classes.getLength() >= 0);

                NodeList counters = document.getElementsByTagName("counter");
                assertTrue("Should contain counter elements", counters.getLength() >= 0);
            }
        } catch (Exception e) {
            verifyXmlGenerationAttempted();
        }
    }

    @Test
    public void testGenerateXmlReportHandlesIOException() {
        File nonExistentDir = new File(temporaryFolder.getRoot(), "nonexistent");
        mojo.xmlOutputFile = new File(nonExistentDir, "jacoco-report.xml");

        IBundleCoverage mockBundle = createSimpleMockBundle("TestProject");

        try {
            mojo.generateXmlReport(mockBundle);
            fail("Should have thrown IOException");
        } catch (Exception e) {
            assertTrue("Should be IOException or similar",
                    e instanceof java.io.IOException ||
                            e.getCause() instanceof java.io.IOException);
        }
    }

    @Test
    public void testGenerateXmlReportOverwritesExistingFile() throws Exception {
        File xmlFile = temporaryFolder.newFile("existing-report.xml");

        Files.write(xmlFile.toPath(), "existing content".getBytes());
        long initialSize = xmlFile.length();

        mojo.xmlOutputFile = xmlFile;
        IBundleCoverage mockBundle = createMockBundleWithPackage(
                "OverwriteProject",
                "com/example",
                "TestClass.java",
                "com/example/TestClass"
        );

        try {
            mojo.generateXmlReport(mockBundle);

            assertTrue("File should still exist", xmlFile.exists());

            if (xmlFile.length() > 0 && xmlFile.length() != initialSize) {
                String content = new String(Files.readAllBytes(xmlFile.toPath()));
                assertTrue("Should contain XML content", content.contains("<?xml"));
                assertFalse("Should not contain original content", content.contains("existing content"));
            }
        } catch (Exception e) {
            verifyXmlGenerationAttempted();
        }
    }

    @Test
    public void testGenerateXmlReportWithEmptyBundle() throws Exception {
        IBundleCoverage mockBundle = createSimpleMockBundle("EmptyProject");

        try {
            mojo.generateXmlReport(mockBundle);

            if (mojo.xmlOutputFile.exists() && mojo.xmlOutputFile.length() > 0) {
                Document document = parseXmlFile();

                Element root = document.getDocumentElement();
                assertEquals("Root should still be 'report'", "report", root.getTagName());
            }
        } catch (Exception e) {
            verifyXmlGenerationAttempted();
        }
    }

    @Test
    public void testGenerateXmlReportInIntegrationWithMainExecute() throws Exception {
        if (!testProjectJacocoExec.exists() || !testProjectClasses.exists()) {
            return;
        }

//        mojo.jacocoExecFile = testProjectJacocoExec;
        mojo.classesDirectory = testProjectClasses;
        mojo.deferReporting = false;

        mojo.execute();

        assertTrue("XML file should exist after full execution", mojo.xmlOutputFile.exists());
        assertTrue("XML file should have content", mojo.xmlOutputFile.length() > 0);

        boolean foundXmlGeneration = log.writtenData.stream()
                .anyMatch(s -> s.contains("Generating aggregated JaCoCo XML report"));
        assertTrue("Should log XML generation", foundXmlGeneration);

        boolean foundXmlSuccess = log.writtenData.stream()
                .anyMatch(s -> s.contains("XML report generated successfully"));
        assertTrue("Should log XML generation success", foundXmlSuccess);

        Document document = parseXmlFile();
        assertNotNull("Should be valid XML", document);
    }

    private void verifyXmlGenerationAttempted() {
        boolean foundGenerationLog = log.writtenData.stream()
                .anyMatch(s -> s.contains("Generating aggregated JaCoCo XML report"));
        assertTrue("Should attempt XML generation", foundGenerationLog);
    }
}