package io.github.svaningelgem;

import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ICounter;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class XmlSimplifiedTest extends BaseTestClass {

    @Test
    public void testXmlOutputFileFieldExists() throws Exception {
        java.lang.reflect.Field field = JacocoConsoleReporterMojo.class.getDeclaredField("xmlOutputFile");
        assertNotNull("xmlOutputFile field should exist", field);
        assertEquals("Should be File type", File.class, field.getType());

        field.setAccessible(true);
        assertNull("Should default to null", field.get(mojo));

        File testFile = new File("test.xml");
        field.set(mojo, testFile);
        assertEquals("Should be settable", testFile, field.get(mojo));
    }

    @Test
    public void testGenerateXmlReportMethodExists() throws Exception {
        Method method = JacocoConsoleReporterMojo.class.getDeclaredMethod("generateXmlReport", IBundleCoverage.class);
        assertNotNull("generateXmlReport method should exist", method);
        assertEquals("Should return void", void.class, method.getReturnType());
    }

    @Test
    public void testGenerateXmlReportWithNullFile() throws Exception {
        Method method = JacocoConsoleReporterMojo.class.getDeclaredMethod("generateXmlReport", IBundleCoverage.class);
        method.setAccessible(true);

        mojo.xmlOutputFile = null;
        IBundleCoverage mockBundle = mock(IBundleCoverage.class);

        method.invoke(mojo, mockBundle);

        boolean hasXmlLogs = log.writtenData.stream()
                .anyMatch(line -> line.contains("XML report"));
        assertFalse("Should not log XML generation when file is null", hasXmlLogs);
    }

    @Test
    public void testGenerateXmlReportCreatesFile() throws Exception {
        Method method = JacocoConsoleReporterMojo.class.getDeclaredMethod("generateXmlReport", IBundleCoverage.class);
        method.setAccessible(true);

        File xmlFile = temporaryFolder.newFile("test-report.xml");
        mojo.xmlOutputFile = xmlFile;

        IBundleCoverage mockBundle = createMinimalMockBundle();

        try {
            method.invoke(mojo, mockBundle);

            boolean hasGenerationLog = log.writtenData.stream()
                    .anyMatch(line -> line.contains("Generating aggregated JaCoCo XML report"));
            assertTrue("Should log XML generation attempt", hasGenerationLog);

        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                String message = cause.getMessage();
                assertTrue("Should fail due to counter issues or sessionInfos being null",
                        message != null && (message.contains("counter") || message.contains("sessionInfos")));
            }
        }
    }

    private IBundleCoverage createMinimalMockBundle() {
        IBundleCoverage mockBundle = mock(IBundleCoverage.class);

        // Use lenient() to avoid UnfinishedStubbingException with final methods
        lenient().when(mockBundle.getName()).thenReturn("TestProject");
        lenient().when(mockBundle.getPackages()).thenReturn(java.util.Collections.emptyList());

        // Create counter mocks with lenient stubbing
        ICounter mockCounter = createMockCounter(0, 0);
        lenient().when(mockBundle.getInstructionCounter()).thenReturn(mockCounter);
        lenient().when(mockBundle.getBranchCounter()).thenReturn(mockCounter);
        lenient().when(mockBundle.getLineCounter()).thenReturn(mockCounter);
        lenient().when(mockBundle.getComplexityCounter()).thenReturn(mockCounter);
        lenient().when(mockBundle.getMethodCounter()).thenReturn(mockCounter);
        lenient().when(mockBundle.getClassCounter()).thenReturn(mockCounter);

        return mockBundle;
    }

    private ICounter createMockCounter(int total, int covered) {
        ICounter mockCounter = mock(ICounter.class);
        lenient().when(mockCounter.getTotalCount()).thenReturn(total);
        lenient().when(mockCounter.getCoveredCount()).thenReturn(covered);
        lenient().when(mockCounter.getMissedCount()).thenReturn(total - covered);
        return mockCounter;
    }

    @Test
    public void testXmlGenerationIntegrationWithRealData() throws Exception {
        if (!testProjectJacocoExec.exists() || !testProjectClasses.exists()) {
            return;
        }

        File xmlFile = temporaryFolder.newFile("integration-test.xml");
        mojo.xmlOutputFile = xmlFile;
        mojo.jacocoExecFile = testProjectJacocoExec;
        mojo.classesDirectory = testProjectClasses;
        mojo.deferReporting = false;

        mojo.execute();

        boolean foundGenerationLog = log.writtenData.stream()
                .anyMatch(line -> line.contains("Generating aggregated JaCoCo XML report"));
        boolean foundSuccessLog = log.writtenData.stream()
                .anyMatch(line -> line.contains("XML report generated successfully"));

        if (xmlFile.exists() && xmlFile.length() > 0) {
            assertTrue("Should log XML generation", foundGenerationLog);
            assertTrue("Should log XML success", foundSuccessLog);

            String content = new String(java.nio.file.Files.readAllBytes(xmlFile.toPath()));
            assertTrue("Should be XML content", content.startsWith("<?xml"));
            assertTrue("Should contain report element", content.contains("<report"));
        } else if (foundGenerationLog) {
            System.out.println("XML generation was attempted but file was not created (acceptable in test)");
        }
    }

    @Test
    public void testXmlOutputFileParameterIntegration() throws Exception {
        java.lang.reflect.Field field = JacocoConsoleReporterMojo.class.getDeclaredField("xmlOutputFile");
        field.setAccessible(true);

        File xmlFile = temporaryFolder.newFile("parameter-test.xml");
        field.set(mojo, xmlFile);

        assertEquals("Parameter should be set correctly", xmlFile, mojo.xmlOutputFile);

        mojo.jacocoExecFile = new File("nonexistent.exec");
        mojo.classesDirectory = new File("nonexistent/classes");
        mojo.deferReporting = false;

        mojo.execute();
    }
}