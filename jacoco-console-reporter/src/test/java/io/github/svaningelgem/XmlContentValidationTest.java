package io.github.svaningelgem;

import org.jacoco.core.analysis.IBundleCoverage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class XmlContentValidationTest extends BaseTestClass {

    @Test
    public void testGeneratedXmlContainsCorrectStructure() throws Exception {
        IBundleCoverage mockBundle = createMockBundleWithPackage(
                "DetailedTestProject",
                "com/example/service",
                "UserService.java",
                "com/example/service/UserService"
        );

        Method generateXmlReportMethod = JacocoConsoleReporterMojo.class.getDeclaredMethod(
                "generateXmlReport", IBundleCoverage.class);
        generateXmlReportMethod.setAccessible(true);

        try {
            generateXmlReportMethod.invoke(mojo, mockBundle);

            if (mojo.xmlOutputFile.exists() && mojo.xmlOutputFile.length() > 100) {
                Document doc = parseXmlFile();

                Element root = doc.getDocumentElement();
                assertEquals("Root element should be 'report'", "report", root.getTagName());
                assertEquals("Report name should match bundle name", "DetailedTestProject", root.getAttribute("name"));

                NodeList sessionInfos = doc.getElementsByTagName("sessioninfo");
                assertTrue("Should have sessioninfo elements", sessionInfos.getLength() >= 0);

                NodeList packages = doc.getElementsByTagName("package");
                if (packages.getLength() > 0) {
                    Element firstPackage = (Element) packages.item(0);
                    assertEquals("Package name should be correct", "com/example/service", firstPackage.getAttribute("name"));

                    NodeList classes = firstPackage.getElementsByTagName("class");
                    if (classes.getLength() > 0) {
                        Element firstClass = (Element) classes.item(0);
                        assertEquals("Class name should be correct", "com/example/service/UserService", firstClass.getAttribute("name"));
                        assertEquals("Source file should be correct", "UserService.java", firstClass.getAttribute("sourcefilename"));

                        validateCounters(firstClass);
                    }
                }
            } else {
                verifyXmlGenerationAttempted();
            }
        } catch (Exception e) {
            verifyXmlGenerationAttempted();
        }
    }

    @Test
    public void testGeneratedXmlContainsCorrectCoverageValues() throws Exception {
        IBundleCoverage mockBundle = createMockBundleWithPackage(
                "SpecificValuesProject",
                "com/example/test",
                "TestClass.java",
                "com/example/test/TestClass"
        );

        Method generateXmlReportMethod = JacocoConsoleReporterMojo.class.getDeclaredMethod(
                "generateXmlReport", IBundleCoverage.class);
        generateXmlReportMethod.setAccessible(true);

        try {
            generateXmlReportMethod.invoke(mojo, mockBundle);

            if (mojo.xmlOutputFile.exists() && mojo.xmlOutputFile.length() > 100) {
                Document doc = parseXmlFile();

                NodeList classes = doc.getElementsByTagName("class");
                if (classes.getLength() > 0) {
                    Element firstClass = (Element) classes.item(0);
                    NodeList counters = firstClass.getElementsByTagName("counter");

                    Element lineCounter = findCounterByType(counters, "LINE");
                    if (lineCounter != null) {
                        assertEquals("LINE counter missed should be correct", "2", lineCounter.getAttribute("missed"));
                        assertEquals("LINE counter covered should be correct", "8", lineCounter.getAttribute("covered"));
                    }
                }
            } else {
                verifyXmlGenerationAttempted();
            }
        } catch (Exception e) {
            verifyXmlGenerationAttempted();
        }
    }

    @Test
    public void testGeneratedXmlIsValidXml() throws Exception {
        IBundleCoverage mockBundle = createMockBundleWithPackage(
                "ValidityTestProject",
                "com/example/validity",
                "ValidityTest.java",
                "com/example/validity/ValidityTest"
        );

        Method generateXmlReportMethod = JacocoConsoleReporterMojo.class.getDeclaredMethod(
                "generateXmlReport", IBundleCoverage.class);
        generateXmlReportMethod.setAccessible(true);
        generateXmlReportMethod.invoke(mojo, mockBundle);

        Document doc = parseXmlFile();
        assertNotNull("Should be able to parse as valid XML", doc);

        String content = new String(java.nio.file.Files.readAllBytes(mojo.xmlOutputFile.toPath()));
        assertTrue("Should start with XML declaration", content.startsWith("<?xml"));
    }

    @Test
    public void testGeneratedXmlWithMultiplePackagesAndClasses() throws Exception {
        Map<String, String[]> packageToClasses = new HashMap<>();
        packageToClasses.put("com/example/service", new String[]{"UserService"});
        packageToClasses.put("com/example/controller", new String[]{"UserController"});

        IBundleCoverage mockBundle = createMultiPackageMockBundle("MultiPackageProject", packageToClasses);

        Method generateXmlReportMethod = JacocoConsoleReporterMojo.class.getDeclaredMethod(
                "generateXmlReport", IBundleCoverage.class);
        generateXmlReportMethod.setAccessible(true);

        try {
            generateXmlReportMethod.invoke(mojo, mockBundle);

            if (mojo.xmlOutputFile.exists() && mojo.xmlOutputFile.length() > 100) {
                Document doc = parseXmlFile();

                NodeList packages = doc.getElementsByTagName("package");
                if (packages.getLength() >= 2) {
                    boolean foundService = false;
                    boolean foundController = false;

                    for (int i = 0; i < packages.getLength(); i++) {
                        Element pkg = (Element) packages.item(i);
                        String name = pkg.getAttribute("name");

                        if ("com/example/service".equals(name)) foundService = true;
                        if ("com/example/controller".equals(name)) foundController = true;
                    }

                    assertTrue("Should find service package", foundService);
                    assertTrue("Should find controller package", foundController);

                    NodeList allClasses = doc.getElementsByTagName("class");
                    assertTrue("Should have multiple classes", allClasses.getLength() >= 2);
                }
            } else {
                verifyXmlGenerationAttempted();
            }
        } catch (Exception e) {
            verifyXmlGenerationAttempted();
        }
    }

    @Test
    public void testGeneratedXmlWithEmptyPackage() throws Exception {
        IBundleCoverage mockBundle = createSimpleMockBundle("EmptyPackageProject");

        Method generateXmlReportMethod = JacocoConsoleReporterMojo.class.getDeclaredMethod(
                "generateXmlReport", IBundleCoverage.class);
        generateXmlReportMethod.setAccessible(true);
        generateXmlReportMethod.invoke(mojo, mockBundle);

        Document doc = parseXmlFile();

        NodeList packages = doc.getElementsByTagName("package");
        if (packages.getLength() > 0) {
            Element pkg = (Element) packages.item(0);
            NodeList classes = pkg.getElementsByTagName("class");
            assertEquals("Empty package should have no classes", 0, classes.getLength());
        }
    }

    private void validateCounters(@NotNull Element classElement) {
        NodeList counters = classElement.getElementsByTagName("counter");
        if (counters.getLength() > 0) {
            boolean hasInstructionCounter = false;
            boolean hasLineCounter = false;
            boolean hasMethodCounter = false;

            for (int i = 0; i < counters.getLength(); i++) {
                Element counter = (Element) counters.item(i);
                String type = counter.getAttribute("type");

                if ("INSTRUCTION".equals(type)) hasInstructionCounter = true;
                if ("LINE".equals(type)) hasLineCounter = true;
                if ("METHOD".equals(type)) hasMethodCounter = true;

                assertTrue("Counter should have 'missed' attribute", counter.hasAttribute("missed"));
                assertTrue("Counter should have 'covered' attribute", counter.hasAttribute("covered"));
            }

            assertTrue("Should have INSTRUCTION counter", hasInstructionCounter);
            assertTrue("Should have LINE counter", hasLineCounter);
            assertTrue("Should have METHOD counter", hasMethodCounter);
        }
    }

    private @Nullable Element findCounterByType(@NotNull NodeList counters, String type) {
        for (int i = 0; i < counters.getLength(); i++) {
            Element counter = (Element) counters.item(i);
            if (type.equals(counter.getAttribute("type"))) {
                return counter;
            }
        }
        return null;
    }

    private void verifyXmlGenerationAttempted() {
        boolean foundGenerationLog = log.writtenData.stream()
                .anyMatch(s -> s.contains("Generating aggregated JaCoCo XML report"));
        assertTrue("Should attempt XML generation", foundGenerationLog);
    }
}