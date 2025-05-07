package io.github.svaningelgem;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

    @Test
    public void testGetOsName() {
        // Get the actual OS name from the system
        String expectedOsName = System.getProperty("os.name").toLowerCase();

        // Test the method
        String result = detector.getOsName();

        // Verify it returns the correct value
        assertEquals(expectedOsName, result);
    }

    @Test
    public void testGetConsoleCP_Windows() {
        Assume.assumeTrue(System.getProperty("os.name").toLowerCase().contains("win"));

        // On Windows, we can test the actual behavior
        int result = detector.getConsoleCP();

        // We can only verify it returns something, not the exact value
        // A common Windows code page would be 437 (US) or 1252 (Western European)
        assertTrue("Console code page should be positive", result > 0);

        // Verify the method was called
        verify(detector).getConsoleCP();
    }

    @Test
    public void testGetConsoleCP_NotWindows() {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().contains("win"));

        // On non-Windows, we need to mock since Kernel32 won't be available
        // This test simply verifies that our spy is correctly set up
        doReturn(1252).when(detector).getConsoleCP();

        int result = detector.getConsoleCP();
        assertEquals(1252, result);
    }

    @Test
    public void testKernel32Instance() {
        Assume.assumeTrue(System.getProperty("os.name").toLowerCase().contains("win"));

        // Test that Kernel32.INSTANCE is available on Windows
        assertNotNull("Kernel32.INSTANCE should not be null on Windows",
                detector.getKernel32Instance());

        // Test that GetConsoleOutputCP can be called
        try {
            int cp = detector.getKernel32Instance().GetConsoleOutputCP();
            assertTrue("Console code page should be positive", cp > 0);
        } catch (Exception e) {
            fail("Calling GetConsoleOutputCP should not throw an exception: " + e.getMessage());
        }
    }

    @Test
    public void testGetConsoleCP_MockedKernel32() {
        // Create a mock of Kernel32
        CharsetDetector.Kernel32 mockKernel32 = mock(CharsetDetector.Kernel32.class);
        when(mockKernel32.GetConsoleOutputCP()).thenReturn(1252);

        // Override the getKernel32Instance method to return our mock
        doReturn(mockKernel32).when(detector).getKernel32Instance();

        // Now test getConsoleCP()
        int result = detector.getConsoleCP();
        assertEquals(1252, result);

        // Verify our mock was called
        verify(mockKernel32).GetConsoleOutputCP();
    }

    @Test
    public void testGetInstanceGetsTheSameInstance() {
        CharsetDetector instance = CharsetDetector.getInstance();
        assertSame(instance, CharsetDetector.getInstance());
    }
}