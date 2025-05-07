package io.github.svaningelgem;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Locale;

import static org.junit.Assert.*;

public class DefaultsTest {
    @Before
    public void setUp() {
        Locale.setDefault(Locale.US);
        resetSingleton();
    }

    @After
    public void tearDown() {
        resetSingleton();
    }

    private void resetSingleton() {
        try {
            Field instanceField = Defaults.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset singleton", e);
        }
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
    public void constructor_withAsciiTrue() {
        Defaults defaults = new Defaults(true);

        assertEquals("| ", defaults.verticalLine);
        assertEquals("+-", defaults.tee);
        assertEquals("\\-", defaults.corner);
    }

    @Test
    public void constructor_withAsciiFalse() {
        Defaults defaults = new Defaults(false);

        assertEquals("│ ", defaults.verticalLine);
        assertEquals("├─", defaults.tee);
        assertEquals("└─", defaults.corner);
    }

    @Test
    public void defaultConstructor_withCharsetDetection() {
        // This test assumes we can't easily mock CharsetDetector.run()
        // So we're just verifying that the constructor runs without error
        // and produces expected initializations
        Defaults defaults = new Defaults();

        // Just verify it's not null - the actual values will depend on CharsetDetector.run()
        assertNotNull(defaults.verticalLine);
        assertNotNull(defaults.tee);
        assertNotNull(defaults.corner);
    }

    @Test
    public void lineFormat_shouldBeCorrectlyFormatted() {
        Defaults defaults = new Defaults(true);

        String expected = "%-" + Defaults.PACKAGE_WIDTH + "s " +
                "| " + "%-" + Defaults.METRICS_WIDTH + "s " +
                "| " + "%-" + Defaults.METRICS_WIDTH + "s " +
                "| " + "%-" + Defaults.METRICS_WIDTH + "s " +
                "| " + "%-" + Defaults.METRICS_WIDTH + "s";

        assertEquals(expected, defaults.lineFormat);
    }

    @Contract("_, _ -> new")
    private static @NotNull String repeat(char ch, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, ch);
        return new String(chars);
    }

    @Test
    public void divider_shouldMatchLineFormat() {
        Defaults defaults = new Defaults(true);

        // Check length matches a filled line and contains only dashes and pipes
        String filled = String.format(defaults.lineFormat,
                repeat('a', Defaults.PACKAGE_WIDTH),
                repeat('b', Defaults.METRICS_WIDTH),
                repeat('c', Defaults.METRICS_WIDTH),
                repeat('d', Defaults.METRICS_WIDTH),
                repeat('e', Defaults.METRICS_WIDTH));

        assertEquals(filled.length(), defaults.divider.length());
        assertTrue(defaults.divider.matches("[-|]+"));
    }
}