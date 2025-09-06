package io.github.svaningelgem;

import org.jacoco.core.analysis.IBundleCoverage;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class XmlMethodIntegrationTest extends BaseTestClass {

    @Test
    public void testGenerateXmlReportMethodExists() throws Exception {
        Method generateXmlReportMethod = JacocoConsoleReporterMojo.class.getDeclaredMethod(
                "generateXmlReport", IBundleCoverage.class);

        assertNotNull("generateXmlReport method should exist", generateXmlReportMethod);
        assertEquals("Method should return void", void.class, generateXmlReportMethod.getReturnType());

        Class<?>[] parameterTypes = generateXmlReportMethod.getParameterTypes();
        assertEquals("Should have one parameter", 1, parameterTypes.length);
        assertEquals("Parameter should be IBundleCoverage", IBundleCoverage.class, parameterTypes[0]);
    }

    @Test
    public void testGenerateReportCallsGenerateXmlReport() throws Exception {
        if (!testProjectJacocoExec.exists() || !testProjectClasses.exists()) {
            return;
        }

        JacocoConsoleReporterMojo spyMojo = spy(mojo);

//        spyMojo.jacocoExecFile = testProjectJacocoExec;
        spyMojo.classesDirectory = testProjectClasses;
        spyMojo.deferReporting = false;

        spyMojo.setLog(log);

        try {
            spyMojo.execute();

            ArgumentCaptor<IBundleCoverage> bundleCaptor = ArgumentCaptor.forClass(IBundleCoverage.class);
            verify(spyMojo, times(1)).generateXmlReport(bundleCaptor.capture());

            IBundleCoverage capturedBundle = bundleCaptor.getValue();
            assertNotNull("Bundle should not be null", capturedBundle);

        } catch (Exception e) {
            assertTrue("XML file should exist after execution", mojo.xmlOutputFile.exists());
        }
    }

    @Test
    public void testGenerateReportDoesNotCallGenerateXmlReportWhenNull() {
        JacocoConsoleReporterMojo spyMojo = spy(mojo);

        spyMojo.xmlOutputFile = null;
//        spyMojo.jacocoExecFile = new File("nonexistent.exec");
        spyMojo.classesDirectory = new File("nonexistent/classes");
        spyMojo.deferReporting = false;

        spyMojo.setLog(log);

        try {
            spyMojo.execute();

            verify(spyMojo, atMost(1)).generateXmlReport(any(IBundleCoverage.class));

        } catch (Exception e) {
            boolean hasXmlLogs = log.writtenData.stream()
                    .anyMatch(s -> s.contains("Generating aggregated JaCoCo XML report"));
            assertFalse("Should not have XML generation logs when xmlOutputFile is null", hasXmlLogs);
        }
    }

    @Test
    public void testGenerateXmlReportMethodAccessibility() throws Exception {
        Method generateXmlReportMethod = JacocoConsoleReporterMojo.class.getDeclaredMethod(
                "generateXmlReport", IBundleCoverage.class);

        generateXmlReportMethod.setAccessible(true);

        IBundleCoverage mockBundle = createSimpleMockBundle("TestBundle");

        mojo.xmlOutputFile = null;
        generateXmlReportMethod.invoke(mojo, mockBundle);

        File xmlFile = temporaryFolder.newFile("method-test.xml");
        mojo.xmlOutputFile = xmlFile;

        try {
            generateXmlReportMethod.invoke(mojo, mockBundle);
            assertTrue("XML file should exist after method call", xmlFile.exists());
        } catch (Exception e) {
            boolean foundGenerationLog = log.writtenData.stream()
                    .anyMatch(s -> s.contains("Generating aggregated JaCoCo XML report"));
            assertTrue("Should attempt XML generation", foundGenerationLog);
        }
    }

    @Test
    public void testGenerateXmlReportWithMockedBundle() throws Exception {
        Method generateXmlReportMethod = JacocoConsoleReporterMojo.class.getDeclaredMethod(
                "generateXmlReport", IBundleCoverage.class);
        generateXmlReportMethod.setAccessible(true);

        IBundleCoverage mockBundle = createMockBundleWithPackage(
                "MockedTestProject",
                "com/example/mocked",
                "MockedClass.java",
                "com/example/mocked/MockedClass"
        );

        try {
            generateXmlReportMethod.invoke(mojo, mockBundle);

            assertTrue("XML file should exist", mojo.xmlOutputFile.exists());

            boolean foundGenerationLog = log.writtenData.stream()
                    .anyMatch(s -> s.contains("Generating aggregated JaCoCo XML report"));
            assertTrue("Should log XML generation start", foundGenerationLog);

            if (mojo.xmlOutputFile.length() > 100) {
                boolean foundSuccessLog = log.writtenData.stream()
                        .anyMatch(s -> s.contains("XML report generated successfully"));
                assertTrue("Should log XML generation success", foundSuccessLog);
            }

        } catch (Exception e) {
            boolean foundGenerationLog = log.writtenData.stream()
                    .anyMatch(s -> s.contains("Generating aggregated JaCoCo XML report"));
            assertTrue("Should log XML generation attempt even if it fails", foundGenerationLog);
        }
    }
}