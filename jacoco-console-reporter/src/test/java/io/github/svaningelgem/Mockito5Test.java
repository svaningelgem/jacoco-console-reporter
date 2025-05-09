package io.github.svaningelgem;

import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class Mockito5Test extends BaseTestClass {

    @Test
    public void testListFilesReturnsNull() throws Exception {
        // Mock directory behavior - use Real method to avoid recursive calls
        File dirMock = spy(new File("some-path"));
        doReturn(true).when(dirMock).exists();
        doReturn(true).when(dirMock).isDirectory();
        doReturn(null).when(dirMock).listFiles(any(FilenameFilter.class));

        // Target directory mock
        File targetDirMock = spy(new File("target-path"));
        doReturn(true).when(targetDirMock).exists();
        doReturn(true).when(targetDirMock).isDirectory();
        doReturn(null).when(targetDirMock).listFiles(any(FilenameFilter.class));

        // Mock File constructor for specific case only
        final AtomicBoolean intercepted = new AtomicBoolean(false);
        try (MockedConstruction<File> mockedFile = Mockito.mockConstruction(
                File.class,
                (mock, context) -> {
                    // Only intercept the specific File(dirMock, "target") call
                    if (context.arguments().size() == 2 &&
                            context.arguments().get(0).equals(dirMock) &&
                            "target".equals(context.arguments().get(1))) {
                        intercepted.set(true);
                        // Don't forward to another spy - directly set behavior
                        lenient().when(mock.exists()).thenReturn(true);
                        lenient().when(mock.isDirectory()).thenReturn(true);
                        lenient().when(mock.listFiles(any(FilenameFilter.class))).thenReturn(null);
                    }
                })) {

            mojo.scanDirectoryForExecFiles(dirMock, null);

            // Verify the interception happened
            assertTrue("File constructor wasn't intercepted", intercepted.get());
        }
    }
}
