package io.github.svaningelgem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.Assert.*;

public class DefaultsTest {
    @Before
    public void setUp() {
        Locale.setDefault(Locale.US);
        Defaults.instance = null;
    }

    @After
    public void tearDown() {
        Defaults.instance = null;
    }

    @Test
    public void truncateMiddle_shouldNotTruncateShortString() {
        String input = "com.example.short";
        String result = Defaults.getInstance().truncateMiddle(input);
        assertEquals(input, result);
    }

    @Test
    public void truncateMiddle_shouldTruncateLongString() {
        String input = "com.example.very.long.package.name.that.exceeds.the.maximum.width";
        String result = Defaults.getInstance().truncateMiddle(input);
        assertEquals(Defaults.PACKAGE_WIDTH, result.length());
        assertTrue(result.contains(".."));
        assertTrue(result.startsWith("com.example"));
        assertTrue(result.endsWith("maximum.width"));
    }

    @Test
    public void formatCoverage_shouldHandleZeroTotal() {
        String result = Defaults.getInstance().formatCoverage(0, 0);
        assertEquals(" ***** (0/0)", result);
    }

    @Test
    public void formatCoverage_shouldFormatPercentageAndRatio() {
        String result = Defaults.getInstance().formatCoverage(75, 100);
        assertEquals("75.00% (75/100)", result);
    }

    @Test
    public void formatCoverage_shouldRoundCorrectly() {
        String result = Defaults.getInstance().formatCoverage(1, 3);
        assertEquals("33.33% (1/3)", result);
    }

    @Test
    public void getInstance_shouldReturnSameInstance() {
        Defaults instance1 = Defaults.getInstance();
        Defaults instance2 = Defaults.getInstance();
        assertSame("getInstance should always return the same instance", instance1, instance2);
    }

    @Test
    public void defaultConstructor_withUtf8Charset() {
        // Create a new Defaults instance with UTF-8
        Defaults defaults = new Defaults(StandardCharsets.UTF_8);

        // Verify UTF-8 characters are used (useAscii will be false)
        assertEquals("│ ", defaults.verticalLine);
        assertEquals("├─", defaults.tee);
        assertEquals("└─", defaults.corner);
    }

    @Test
    public void defaultConstructor_withNonUtf8Charset() {
        // Create a new Defaults instance with a non-UTF-8 charset
        Defaults defaults = new Defaults(StandardCharsets.ISO_8859_1);

        // Verify ASCII characters are used (useAscii will be true)
        assertEquals("| ", defaults.verticalLine);
        assertEquals("+-", defaults.tee);
        assertEquals("\\-", defaults.corner);
    }

    @Test
    public void lineFormat_shouldBeCorrectlyFormatted() {
        Defaults defaults = new Defaults();

        String expected = "%-" + Defaults.PACKAGE_WIDTH + "s " +
                "| " + "%-" + Defaults.METRICS_WIDTH + "s " +
                "| " + "%-" + Defaults.METRICS_WIDTH + "s " +
                "| " + "%-" + Defaults.METRICS_WIDTH + "s " +
                "| " + "%-" + Defaults.METRICS_WIDTH + "s";

        assertEquals(expected, defaults.lineFormat);
    }

    @Test
    public void divider_shouldMatchLineFormat() {
        Defaults defaults = new Defaults();

        // Check length and pattern of divider
        String filled = String.format(defaults.lineFormat, "X", "X", "X", "X", "X");

        assertEquals(filled.length(), defaults.divider.length());
        assertTrue(defaults.divider.matches("[-|]+"));
    }
}