package io.github.svaningelgem;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultsTest {

    @Test
    public void truncateMiddle_shouldNotTruncateShortString() {
        String input = "com.example.short";

        String result = Defaults.truncateMiddle(input);

        assertEquals(input, result);
    }

    @Test
    public void truncateMiddle_shouldTruncateLongString() {
        String input = "com.example.very.long.package.name.that.exceeds.the.maximum.width";

        String result = Defaults.truncateMiddle(input);

        assertEquals(Defaults.PACKAGE_WIDTH, result.length());
        assertTrue(result.contains(".."));
        assertTrue(result.startsWith("com.example"));
        assertTrue(result.endsWith("maximum.width"));
    }

    @Test
    public void formatCoverage_shouldHandleZeroTotal() {
        String result = Defaults.formatCoverage(0, 0);

        assertEquals(" ***** (0/0)", result);
    }

    @Test
    public void formatCoverage_shouldFormatPercentageAndRatio() {
        String result = Defaults.formatCoverage(75, 100);

        assertEquals("75.00% (75/100)", result.replace(',', '.'));
    }

    @Test
    public void formatCoverage_shouldRoundCorrectly() {
        String result = Defaults.formatCoverage(1, 3);

        assertEquals("33.33% (1/3)", result.replace(',', '.'));
    }
}