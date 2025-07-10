package io.github.svaningelgem;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class BaseTestClass {
    protected final static Random RANDOM = new Random();

    protected static final String TEST_GROUP = "test.group";
    protected static final String TEST_ARTIFACT = "test.artifact";

    @Rule
    public final MojoRule rule = new MojoRule();

    protected JacocoConsoleReporterMojo mojo;
    protected MyLog log;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected final File mainProjectDir = new File(".").getAbsoluteFile();
    protected final File mainProjectClasses = new File(mainProjectDir, "target/classes");
    protected final File testProjectDir = new File("../test-project").getAbsoluteFile();
    protected final File testProjectJacocoExec = new File(testProjectDir, "target/jacoco.exec");
    protected final File testProjectClasses = new File(testProjectDir, "target/classes");
    protected final File pom = new File(getBasedir(), "src/test/resources/unit/pom.xml");

    private int fileCounter = 0;

    protected String getBasedir() {
        return System.getProperty("basedir", new File("").getAbsolutePath());
    }

    @Before
    public void setUp() throws Exception {
        Locale.setDefault(Locale.US);
        Defaults.instance = null;

        fileCounter = 0;

        PlexusContainer container = rule.getContainer();
        JacocoConsoleReporterMojo customMojo = new JacocoConsoleReporterMojo();

        // Register it with the container - directly using a role hint
        String roleHint = "io.github.svaningelgem:jacoco-console-reporter:report";
        container.addComponent(customMojo, Mojo.class, roleHint);

        mojo = (JacocoConsoleReporterMojo) container.lookup(Mojo.class, roleHint);

        // Create a real project with JaCoCo plugin
        MavenProject project = createProjectWithJacocoPlugin(null);
        project.setFile(pom.getParentFile());

        mojo.project = project;
        mojo.mavenSession = createRealMavenSession(Collections.singletonList(project));

        mojo.jacocoExecFile = new File(project.getBuild().getDirectory(), "jacoco.exec").getCanonicalFile();
        mojo.classesDirectory = new File(project.getBuild().getOutputDirectory()).getCanonicalFile();
        mojo.deferReporting = true;
        mojo.showFiles = false;
        mojo.showTree = true;
        mojo.showSummary = true;
        mojo.scanModules = false;
        mojo.weightClassCoverage = 0.1;
        mojo.weightMethodCoverage = 0.1;
        mojo.weightBranchCoverage = 0.4;
        mojo.weightLineCoverage = 0.4;
        mojo.ignoreFilesInBuildDirectory = true;
        mojo.targetDir = new File(project.getBuild().getDirectory()).getCanonicalFile();
        mojo.baseDir = project.getBasedir();

        log = new MyLog();
        mojo.setLog(log);
    }

    @After
    public void tearDown() {
        JacocoConsoleReporterMojo.collectedClassesPaths.clear();
        JacocoConsoleReporterMojo.collectedExecFilePaths.clear();
        JacocoConsoleReporterMojo.collectedExcludePatterns.clear();
        JacocoConsoleReporterMojo.collectedSonarExcludePatterns.clear();
    }

    private static int nextInt(int bound) {
        if (bound == 0) return 0;

        return RANDOM.nextInt(bound);
    }

    @Contract(" -> new")
    protected static @NotNull CoverageMetrics getRandomCoverage() {
        CoverageMetrics cm = new CoverageMetrics();

        cm.totalClasses = nextInt(100);
        cm.coveredClasses = nextInt(cm.totalClasses);
        cm.totalMethods = nextInt(100);
        cm.coveredMethods = nextInt(cm.totalMethods);
        cm.totalLines = nextInt(100);
        cm.coveredLines = nextInt(cm.totalLines);
        cm.totalBranches = nextInt(100);
        cm.coveredBranches = nextInt(cm.totalBranches);

        return cm;
    }

    protected void assertLogNotContains(@NotNull String @NotNull [] expected) {
        try {
            assertLogContains(expected);
        } catch (AssertionError e) {
            return; // Good!
        }

        failLog(expected, "Expected log to NOT contain:");
    }
    protected void assertLogContains(@NotNull String @NotNull [] expected) {
        assertTrue("Wrong test: we need SOMETHING to check!", expected.length > 0);

        int line = 0;
        while (line < log.writtenData.size() && !log.writtenData.get(line).contains(expected[0])) {
            line++;
        }

        if (line == log.writtenData.size()) {
            fail("I couldn't find the initial line even: " + expected[0]);
        }

        final int begin = line;
        assertTrue("We should have at least enough lines left to check the whole expected array", line <= log.writtenData.size() - expected.length);
        for (; line < log.writtenData.size() && line - begin < expected.length; line++) {
            String expectedAsUtf8 = expected[line - begin];
            String expectedAsAscii = expectedAsUtf8.replace("│", "|").replace("├─", "+-").replace("└─", "\\-");

            String currentLine = log.writtenData.get(line);
            if (currentLine.startsWith(expectedAsUtf8) || currentLine.startsWith(expectedAsAscii)) {
                continue;
            }

            failLog(expected);
        }
    }

    protected void failLog(String @NotNull [] expected) {
        failLog(expected, "Expected log to contain:");
    }

    protected void failLog(String @NotNull [] expected, String message) {
        StringBuilder builder = new StringBuilder();
        builder.append(message).append('\n');
        for (String line : expected) {
            builder.append(line).append("\n");
        }
        builder.append("\n");
        builder.append("Actual log:\n");
        for (String line : log.writtenData) {
            builder.append(line).append("\n");
        }
        builder.append("\n");

        fail(builder.toString());
    }

    /**
     * Create the tree starting from the root, with the names
     * Returns the last created DirectoryNode
     */
    protected void createTree(DirectoryNode root, int amountOfFiles, @Nullable CoverageMetrics defaultCoverage, @Nullable String @Nullable ... names) {
        if (names != null) {
            for (String name : names) {
                if (name == null) {
                    continue;
                }

                root = root.getSubdirectories().computeIfAbsent(name, DirectoryNode::new);
            }
        }

        // Adding debug files
        List<String> fileNames = new ArrayList<>();
        for (int i = 0; i < amountOfFiles; i++) {
            fileNames.add("Example" + (fileCounter++) + ".java");
        }

        addFiles(root, defaultCoverage, fileNames.toArray(new String[0]));
    }

    protected void createTree(DirectoryNode root, int amountOfFiles, @Nullable String @Nullable ... names) {
        createTree(root, amountOfFiles, null, names);
    }

    protected void addFiles(DirectoryNode toNode, @Nullable CoverageMetrics defaultCoverage, @Nullable String @Nullable ... names) {
        if (names == null) {
            return;
        }

        for (String name : names) {
            if (name == null) {
                continue;
            }

            SourceFileNode file = new SourceFileNode(name, defaultCoverage == null ? getRandomCoverage() : defaultCoverage.clone());
            toNode.getSourceFiles().add(file);
        }
    }

    /**
     * Helper method to copy a directory
     */
    protected void copyDirectory(@NotNull File sourceDir, File destDir) throws IOException {
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            return;
        }

        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        File[] files = sourceDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            File destFile = new File(destDir, file.getName());
            if (file.isDirectory()) {
                copyDirectory(file, destFile);
            } else {
                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Helper method to copy a resource from the classpath to a file
     */
    protected void copyResourceToFile(String resourcePath, File destFile) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Files.copy(is, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Creates a real MavenProject with JaCoCo plugin configuration
     */
    @Contract("_ -> new")
    protected @NotNull MavenProject createProjectWithJacocoPlugin(String destFile) throws IOException {
        Model model = new Model();
        model.setGroupId("test.group");
        model.setArtifactId("test-artifact");
        model.setVersion("1.0.0");

        File outputDir = new File(temporaryFolder.getRoot(), "output").getAbsoluteFile();
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        Build build = new Build();
        build.setOutputDirectory(outputDir.toString());
        build.setDirectory(temporaryFolder.getRoot().getAbsolutePath());
        model.setBuild(build);

        Plugin plugin = new Plugin();
        plugin.setGroupId("org.jacoco");
        plugin.setArtifactId("jacoco-maven-plugin");
        plugin.setVersion("0.8.12");

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        if (destFile != null) {
            Xpp3Dom destFileNode = new Xpp3Dom("destFile");
            destFileNode.setValue(destFile);
            configuration.addChild(destFileNode);
        }

        Xpp3Dom excludes = new Xpp3Dom("excludes");
        configuration.addChild(excludes);

        Xpp3Dom exclude = new Xpp3Dom("exclude");
        exclude.setValue("com/example/generated");
        excludes.addChild(exclude);

        plugin.setConfiguration(configuration);

        build.addPlugin(plugin);

        return new MavenProject(model);
    }

    /**
     * Creates a real MavenSession with multiple projects
     */
    @SuppressWarnings("deprecation")
    protected @NotNull MavenSession createRealMavenSession(List<MavenProject> projects) {
        PlexusContainer container = rule.getContainer();
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        return new MavenSession(
                container,
                request,
                new DefaultMavenExecutionResult(),
                projects
        );
    }

    @Contract("_, _, _ -> new")
    protected @NotNull Plugin createPlugin(@NotNull String groupId, @NotNull String artifactId, @Nullable String xml) {
        return createPlugin(groupId, artifactId, parseXml(xml));
    }

    @Contract("_, _, _ -> new")
    protected @NotNull Plugin createPlugin(@NotNull String groupId, @NotNull String artifactId, @Nullable Xpp3Dom configuration) {
        Plugin plugin = new Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);
        if (configuration != null) {
            if ("configuration".equals(configuration.getName())) {
                plugin.setConfiguration(configuration);
            } else {
                Xpp3Dom configurationNode = new Xpp3Dom("configuration");
                configurationNode.addChild(configuration);
                plugin.setConfiguration(configurationNode);
            }
        }

        return plugin;
    }

    protected @NotNull Plugin createPlugin(@Nullable Xpp3Dom configuration) {
        return createPlugin(TEST_GROUP, TEST_ARTIFACT, configuration);
    }

    @Contract("_ -> new")
    protected @NotNull Plugin createPlugin(@Nullable String xml) {
        return createPlugin(parseXml(xml));
    }

    protected @Nullable Xpp3Dom parseXml(@Nullable String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return null;
        }

        try {
            // Wrap the XML content in a configuration element
            String wrappedXml = "<configuration>" + xml + "</configuration>";
            Xpp3Dom config = Xpp3DomBuilder.build(new StringReader(wrappedXml));

            // If there is only one child element, and it's a "configuration", unwrap it
            if (config.getChildCount() == 1 && "configuration".equals(config.getChild(0).getName())) {
                return config.getChild(0);
            }

            return config;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML: " + xml, e);
        }
    }

    protected void createFile(File parent, String path, String content) throws IOException {
        File file = new File(parent, path);
        file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }

    }

    /**
     * Asserts that two Pattern objects are equal based on pattern string
     */
    public static void assertPatternEquals(String expected, Pattern actual) {
        if (expected == null || actual == null) {
            fail("One pattern is null");
        }

        boolean patternsEqual = expected.equals(actual.pattern());

        if (!patternsEqual) {
            fail(String.format("Patterns not equal: expected='%s', actual='%s'", expected, actual.pattern()));
        }
    }

    /**
     * Asserts that two collections of Pattern objects are equal
     */
    public static void assertPatternEquals(Collection<String> expected, Collection<Pattern> actual) {
        if (expected == null || actual == null) {
            fail("One collection is null");
        }

        // Convert to lists and sort by pattern and flags for consistent comparison
        List<String> expectedPatterns = new ArrayList<>(expected).stream()
                .sorted()
                .collect(Collectors.toList());

        List<String> actualPatterns = actual.stream()
                .map(Pattern::pattern)
                .sorted()
                .collect(Collectors.toList());

        assertEquals(expectedPatterns, actualPatterns);
    }
}
