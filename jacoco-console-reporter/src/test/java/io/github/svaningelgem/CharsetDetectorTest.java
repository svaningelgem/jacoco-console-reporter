package io.github.svaningelgem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class CharsetDetectorTest {

    @Spy
    private CharsetDetector detector;

    @Before
    public void setUp() {
        CharsetDetector.instance = null;
        detector = spy(CharsetDetector.getInstance());
    }

    @After
    public void tearDown() {
        CharsetDetector.instance = null;
    }

    @Test
    public void testGetCharsetOnNonWindows() {
        // Mock the OS name to be Linux
        doReturn("linux").when(detector).getOsName();

        // Test
        Charset result = detector.getCharset();
        assertEquals(StandardCharsets.UTF_8, result);
    }

    @Test
    public void testGetCharsetOnWindowsWithUtf8CodePage() {
        // Mock Windows OS and UTF-8 code page
        doReturn("windows").when(detector).getOsName();
        doReturn(65001).when(detector).getConsoleCP();

        // Test
        Charset result = detector.getCharset();
        assertEquals(StandardCharsets.UTF_8, result);
    }

    @Test
    public void testGetCharsetOnWindowsWithValidCodePage() {
        // Mock Windows OS and a valid code page
        doReturn("windows").when(detector).getOsName();
        doReturn(1252).when(detector).getConsoleCP();

        // Let the real getCharsetForCodePage method run

        // Test
        Charset result = detector.getCharset();
        assertEquals(Charset.forName("CP1252"), result);
    }

    @Test
    public void testGetCharsetOnWindowsWithInvalidCodePage() {
        // Mock Windows OS and an invalid code page
        doReturn("windows").when(detector).getOsName();
        doReturn(-1).when(detector).getConsoleCP();

        // Test
        Charset result = detector.getCharset();
        assertEquals(StandardCharsets.UTF_8, result);
    }

    @Test
    public void testGetCharsetForCodePageValid() {
        Charset result = detector.getCharsetForCodePage(1252);
        assertEquals(Charset.forName("CP1252"), result);
    }

    @Test
    public void testGetCharsetForCodePageUtf8() {
        Charset result = detector.getCharsetForCodePage(65001);
        assertEquals(StandardCharsets.UTF_8, result);
    }

    @Test
    public void testGetCharsetForCodePageInvalid() {
        // Test with an invalid code page - should return UTF-8 as fallback
        Charset result = detector.getCharsetForCodePage(-1);
        assertEquals(StandardCharsets.UTF_8, result);
    }
}