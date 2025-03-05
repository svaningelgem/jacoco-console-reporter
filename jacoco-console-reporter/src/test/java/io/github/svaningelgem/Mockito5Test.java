package io.github.svaningelgem;

import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class Mockito5Test {

    @Rule
    public final MojoRule rule = new MojoRule();

    @Test
    public void testListFilesReturnsNull() throws Exception {
        File pom = new File(System.getProperty("basedir", new File("").getAbsolutePath()), "src/test/resources/unit/pom.xml");
        JacocoConsoleReporterMojo mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        Method scanDirectoryForExecFiles = JacocoConsoleReporterMojo.class.getDeclaredMethod("scanDirectoryForExecFiles", File.class, List.class);
        scanDirectoryForExecFiles.setAccessible(true);

        // Mock directory behavior
        File dirMock = mock(File.class);
        doReturn(true).when(dirMock).exists();
        doReturn(true).when(dirMock).isDirectory();
        doReturn(null).when(dirMock).listFiles(any(FilenameFilter.class));

        File targetDirMock = mock(File.class);
        doReturn(true).when(targetDirMock).exists();
        doReturn(true).when(targetDirMock).isDirectory();
        doReturn(null).when(targetDirMock).listFiles(any(FilenameFilter.class));

        // Mock new File creation
        try (MockedConstruction<File> mockedFile = Mockito.mockConstruction(
                File.class,
                (mock, context) -> {
                    // If constructor is called with dirMock and "target", return targetDirMock
                    if (context.arguments().size() == 2 &&
                            context.arguments().get(0) == dirMock &&
                            context.arguments().get(1).equals("target")) {
                        when(mock.exists()).thenReturn(true);
                        when(mock.isDirectory()).thenReturn(true);
                        when(mock.listFiles(any(FilenameFilter.class))).thenReturn(null);
                        // Make the mock behave like targetDirMock
                        lenient().doReturn(targetDirMock).when(mock);
                    }
                })) {

            scanDirectoryForExecFiles.invoke(mojo, dirMock, null);
        }
    }
}
