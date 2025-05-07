package io.github.svaningelgem;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public class Defaults {
    private static final boolean USE_ASCII;

    // Define column widths
    static final int PACKAGE_WIDTH = 50;
    static final int METRICS_WIDTH = 20;

    // Define tree characters based on terminal capabilities

    static final String LAST_DIR_SPACE = "  ";
    static final String VERTICAL_LINE;
    static final String TEE;
    static final String CORNER;

    static final String LINE_FORMAT;
    static final String DIVIDER;

    static {
        USE_ASCII = CharsetDetector.run() != StandardCharsets.UTF_8;

        VERTICAL_LINE = USE_ASCII ? "| " : "│ ";
        TEE = USE_ASCII ? "+-" : "├─";
        CORNER = USE_ASCII ? "\\-" : "└─";
        LINE_FORMAT = "%-" + PACKAGE_WIDTH + "s " + VERTICAL_LINE + "%-" + METRICS_WIDTH + "s " + VERTICAL_LINE + "%-" + METRICS_WIDTH + "s " + VERTICAL_LINE + "%-" + METRICS_WIDTH + "s " + VERTICAL_LINE + "%-" + METRICS_WIDTH + "s";
        DIVIDER = String.format(Defaults.LINE_FORMAT, "", "", "", "", "").replace(' ', '-');
    }

    /**
     * Truncates a string in the middle if it exceeds maxLength
     * Example: "com.example.very.long.package.name" -> "com.example...kage.name"
     */
    static @NotNull String truncateMiddle(@NotNull String input) {
        if (input.length() <= PACKAGE_WIDTH) {
            return input;
        }

        int prefixLength = (PACKAGE_WIDTH - 2) / 2;
        int suffixLength = PACKAGE_WIDTH - 2 - prefixLength;

        return input.substring(0, prefixLength) + ".." +
                input.substring(input.length() - suffixLength);
    }

    /**
     * Formats coverage metrics as a percentage with covered/total values.
     *
     * @param covered Number of covered items
     * @param total   Total number of items
     * @return Formatted string showing percentage and ratio (e.g., "75.00% (3/4)")
     */
    @Contract(pure = true)
    static @NotNull String formatCoverage(double covered, double total) {
        if (total <= 0) return " ***** (0/0)";
        double percentage = covered / total * 100;
        return String.format("%5.2f%% (%d/%d)", percentage, (int)covered, (int)total);
    }

    private Defaults() { }
}
