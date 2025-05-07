package io.github.svaningelgem;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest({System.class, CharsetDetector.Kernel32.class, Charset.class})
public class CharsetDetectorTest {

    @Test
    public void run_shouldReturnUtf8OnNonWindowsSystem() {
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getProperty("os.name")).thenReturn("Linux");

        Charset result = new CharsetDetector().run();

        assertEquals(StandardCharsets.UTF_8, result);
    }

    @Test
    public void run_shouldReturnUtf8OnWindowsWithUtf8CodePage() {
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getProperty("os.name")).thenReturn("Windows 10");

        PowerMockito.mockStatic(CharsetDetector.Kernel32.class);
        PowerMockito.when(CharsetDetector.Kernel32.INSTANCE.GetConsoleOutputCP()).thenReturn(65001);

        Charset result = new CharsetDetector().run();

        assertEquals(StandardCharsets.UTF_8, result);
    }

    @Test
    public void run_shouldReturnCorrectCharsetOnWindowsWithNonUtf8CodePage() {
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getProperty("os.name")).thenReturn("Windows 10");

        PowerMockito.mockStatic(CharsetDetector.Kernel32.class);
        PowerMockito.when(CharsetDetector.Kernel32.INSTANCE.GetConsoleOutputCP()).thenReturn(437); // DOS Latin US

        Charset result = new CharsetDetector().run();

        assertEquals(Charset.forName("CP437"), result);
    }

    @Test
    public void run_shouldFallbackToUtf8OnWindowsWithInvalidCodePage() {
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getProperty("os.name")).thenReturn("Windows 10");

        PowerMockito.mockStatic(CharsetDetector.Kernel32.class);
        PowerMockito.when(CharsetDetector.Kernel32.INSTANCE.GetConsoleOutputCP()).thenReturn(999999); // Invalid code page

        PowerMockito.mockStatic(Charset.class);
        PowerMockito.when(Charset.forName("CP999999")).thenThrow(new IllegalArgumentException("Unknown charset"));

        Charset result = new CharsetDetector().run();

        assertEquals(StandardCharsets.UTF_8, result);
    }
}
