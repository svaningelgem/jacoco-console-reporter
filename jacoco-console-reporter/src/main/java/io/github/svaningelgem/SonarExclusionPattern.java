package io.github.svaningelgem;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Represents a Sonar exclusion pattern with its source project context
 */
@Data
@RequiredArgsConstructor
public class SonarExclusionPattern {
    /**
     * The original Sonar pattern (file-based)
     */
    private final String originalPattern;

    /**
     * The project this pattern was found in
     */
    private final MavenProject sourceProject;

    /**
     * Compiled regex pattern for matching
     */
    private Pattern compiledPattern;

    /**
     * Checks if a file path (relative to the source project) matches this exclusion pattern
     */
    public boolean matches(@NotNull String filePath, @NotNull MavenProject currentProject) {
        if (compiledPattern == null) {
            compiledPattern = compilePattern();
        }

        // Convert the file path to be relative to the source project if needed
        String relativePath = getRelativePath(filePath, currentProject);

        return compiledPattern.matcher(relativePath).matches();
    }

    /**
     * Gets the file path relative to the source project
     */
    @NotNull String getRelativePath(@NotNull String filePath, @NotNull MavenProject currentProject) {
        if (currentProject.equals(sourceProject)) {
            return filePath;
        }

        try {
            File sourceBaseDir = sourceProject.getBasedir();
            File currentBaseDir = currentProject.getBasedir();

            if (sourceBaseDir != null && currentBaseDir != null) {
                Path sourcePath = sourceBaseDir.toPath();
                Path currentPath = currentBaseDir.toPath();
                Path relativePath = sourcePath.relativize(currentPath);

                return relativePath.resolve(filePath).toString().replace('\\', '/');
            }
        } catch (Exception e) {
            // Fall back to the original path if relativization fails
        }

        return filePath;
    }

    /**
     * Compiles the Sonar pattern into a regex Pattern
     */
    @NotNull Pattern compilePattern() {
        String pattern = originalPattern.replace("\\", "/");

        // Escape regex special characters except for our wildcards
        pattern = pattern
                .replace(".", "\\.")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("+", "\\+")
                .replace("|", "\\|");

        // Handle wildcards with proper precedence - use placeholders to avoid interference
        // First handle **/ (directory wildcard)
        pattern = pattern.replace("**/", "__DOUBLE_STAR_SLASH__");

        // Then handle trailing ** (matches everything remaining)
        pattern = pattern.replaceAll("\\*\\*$", "__TRAILING_DOUBLE_STAR__");

        // Handle remaining ** in the middle (treat as directory wildcard)
        pattern = pattern.replace("**", "__DOUBLE_STAR_SLASH__");

        // Handle single * (filename wildcard)
        pattern = pattern.replace("*", "__STAR__");

        // Convert placeholders to regex
        pattern = pattern
                .replace("__DOUBLE_STAR_SLASH__", "(?:[^/]*/)*")
                .replace("__TRAILING_DOUBLE_STAR__", "(?:[^/]*/)*(?:[^/]*)")
                .replace("__STAR__", "[^/]*");

        // Anchor the pattern
        pattern = "^" + pattern + "$";

        return Pattern.compile(pattern);
    }
}