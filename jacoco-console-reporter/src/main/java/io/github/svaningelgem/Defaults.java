package io.github.svaningelgem;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Defaults {
    // Define column widths
    final static int PACKAGE_WIDTH = 50;
    final static int METRICS_WIDTH = 20;

    final static int PACKAGE_WIDTH_WITH_MISSING = 30;
    final static int MISSING_WIDTH = 30;

    private final boolean useAscii;

    // Define tree characters based on terminal capabilities
    final String lastDirSpace = "  ";
    final String verticalLine;
    final String tee;
    final String corner;

    final String lineFormat;
    final String divider;

    final String lineFormatWithMissing;
    final String dividerWithMissing;

    static Defaults instance = null;

    public static Defaults getInstance() {
        if (instance == null) {
            instance = new Defaults();
        }
        return instance;
    }

    public Defaults() {
        this(CharsetDetector.getInstance().getCharset());
    }

    public Defaults(Charset currentCharset) {
        useAscii = currentCharset != StandardCharsets.UTF_8;

        verticalLine = this.useAscii ? "| " : "│ ";
        tee = this.useAscii ? "+-" : "├─";
        corner = this.useAscii ? "\\-" : "└─";
        lineFormat = "%-" + PACKAGE_WIDTH + "s " + verticalLine + "%-" + METRICS_WIDTH + "s " + verticalLine + "%-" + METRICS_WIDTH + "s " + verticalLine + "%-" + METRICS_WIDTH + "s " + verticalLine + "%-" + METRICS_WIDTH + "s";
        divider = String.format(lineFormat, "", "", "", "", "").replace(' ', '-');

        lineFormatWithMissing = "%-" + PACKAGE_WIDTH_WITH_MISSING + "s " + verticalLine +
                "%-" + METRICS_WIDTH + "s " + verticalLine +
                "%-" + METRICS_WIDTH + "s " + verticalLine +
                "%-" + METRICS_WIDTH + "s " + verticalLine +
                "%-" + METRICS_WIDTH + "s " + verticalLine +
                "%-" + MISSING_WIDTH + "s";
        dividerWithMissing = String.format(lineFormatWithMissing, "", "", "", "", "", "").replace(' ', '-');
    }

    /**
     * Truncates a string in the middle if it exceeds maxLength
     * Example: "com.example.very.long.package.name" -> "com.example...kage.name"
     */
    @NotNull String truncateMiddle(@NotNull String input) {
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
    @NotNull String formatCoverage(double covered, double total) {
        if (total <= 0) return " ***** (0/0)";
        double percentage = covered / total * 100;
        return String.format("%5.2f%% (%d/%d)", percentage, (int) covered, (int) total);
    }

    String truncateMiddleForMissing(@NotNull String input) {
        if (input.length() <= PACKAGE_WIDTH_WITH_MISSING) {
            return input;
        }

        int prefixLength = (PACKAGE_WIDTH_WITH_MISSING - 2) / 2;
        int suffixLength = PACKAGE_WIDTH_WITH_MISSING - 2 - prefixLength;

        return input.substring(0, prefixLength) + ".." +
                input.substring(input.length() - suffixLength);
    }

    String truncateMissing(@NotNull String missing) {
        if (missing.length() <= MISSING_WIDTH) {
            return missing;
        }

        // Show the first part of missing lines
        return missing.substring(0, MISSING_WIDTH - 3) + "...";
    }
}
