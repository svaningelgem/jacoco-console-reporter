package io.github.svaningelgem;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class Defaults {
    private static final boolean USE_ASCII = false;  // disabled for now

    // Define column widths
    static final int PACKAGE_WIDTH = 50;
    static final int METRICS_WIDTH = 20;

    // Define tree characters based on terminal capabilities
    static final String LAST_DIR_SPACE = "  ";
    static final String VERTICAL_LINE = USE_ASCII ? "| " : "│ ";
    static final String TEE = USE_ASCII ? "+-" : "├─";
    static final String CORNER = USE_ASCII ? "\\-" : "└─";

    static final String DIVIDER = getDivider();
    static final String LINE_FORMAT = "%-" + PACKAGE_WIDTH + "s " + VERTICAL_LINE + "%-" + METRICS_WIDTH + "s " + VERTICAL_LINE + "%-" + METRICS_WIDTH + "s " + VERTICAL_LINE + "%-" + METRICS_WIDTH + "s " + VERTICAL_LINE + "%-" + METRICS_WIDTH + "s";

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

    /**
     * Build a divider with certain widths
     */
    private static @NotNull String getDivider() {
        return String.format(Defaults.LINE_FORMAT, "", "", "", "", "").replace(' ', '-');
    }

    private Defaults() { }
}
