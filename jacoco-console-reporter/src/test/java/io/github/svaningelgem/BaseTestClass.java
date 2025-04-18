package io.github.svaningelgem;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BaseTestClass {
    protected final static Random RANDOM = new Random();

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

    protected Method scanDirectoryForExecFiles;
    protected Method generateReport;
    protected Method analyzeCoverage;
    protected Method printTree;
    protected Method printSummary;
    protected Method getConfiguredExecFilePatterns;
    protected Method shouldReport;
    protected Method buildDirectoryTree;
    protected Method loadExecutionData;
    protected Method loadExecFile;

    protected String getBasedir() {
        return System.getProperty("basedir", new File("").getAbsolutePath());
    }

    @Before
    public void setUp() throws Exception {
        Locale.setDefault(Locale.US);

        fileCounter = 0;

        mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        // Setting the defaults
        mojo.deferReporting = true;
        mojo.showFiles = false;
        mojo.showTree = true;
        mojo.showSummary = true;
        mojo.scanModules = false;
        mojo.weightClassCoverage = 0.1;
        mojo.weightMethodCoverage = 0.1;
        mojo.weightBranchCoverage = 0.4;
        mojo.weightLineCoverage = 0.4;

        // Create a real project with JaCoCo plugin
        MavenProject project = createProjectWithJacocoPlugin(null);
        mojo.project = project;

        // Create a real MavenSession with this as the only project
        List<MavenProject> projects = new ArrayList<>();
        projects.add(project);
        mojo.mavenSession = createRealMavenSession(projects);

        // Configure mojo
        mojo.baseDir = temporaryFolder.getRoot();

        log = new MyLog();
        mojo.setLog(log);

        reflectOnMethods();
    }

    private void reflectOnMethods() throws NoSuchMethodException {
        scanDirectoryForExecFiles = JacocoConsoleReporterMojo.class.getDeclaredMethod("scanDirectoryForExecFiles", File.class, List.class);
        scanDirectoryForExecFiles.setAccessible(true);
        generateReport = JacocoConsoleReporterMojo.class.getDeclaredMethod("generateReport");
        generateReport.setAccessible(true);
        printTree = JacocoConsoleReporterMojo.class.getDeclaredMethod("printTree", DirectoryNode.class);
        printTree.setAccessible(true);
        analyzeCoverage = JacocoConsoleReporterMojo.class.getDeclaredMethod("analyzeCoverage", org.jacoco.core.data.ExecutionDataStore.class);
        analyzeCoverage.setAccessible(true);
        getConfiguredExecFilePatterns = JacocoConsoleReporterMojo.class.getDeclaredMethod("getConfiguredExecFilePatterns");
        getConfiguredExecFilePatterns.setAccessible(true);
        shouldReport = JacocoConsoleReporterMojo.class.getDeclaredMethod("shouldReport");
        shouldReport.setAccessible(true);
        loadExecFile = JacocoConsoleReporterMojo.class.getDeclaredMethod("loadExecFile", File.class, org.jacoco.core.data.ExecutionDataStore.class, org.jacoco.core.data.SessionInfoStore.class);
        loadExecFile.setAccessible(true);
        loadExecutionData = JacocoConsoleReporterMojo.class.getDeclaredMethod("loadExecutionData");
        loadExecutionData.setAccessible(true);
        buildDirectoryTree = JacocoConsoleReporterMojo.class.getDeclaredMethod("buildDirectoryTree", org.jacoco.core.analysis.IBundleCoverage.class);
        buildDirectoryTree.setAccessible(true);
        printSummary = JacocoConsoleReporterMojo.class.getDeclaredMethod("printSummary", DirectoryNode.class);
        printSummary.setAccessible(true);
    }

    @After
    public void tearDown() {
        JacocoConsoleReporterMojo.collectedClassesPaths.clear();
        JacocoConsoleReporterMojo.collectedExecFilePaths.clear();
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
        for (; line < log.writtenData.size(); line++) {
            if (!log.writtenData.get(line).startsWith(expected[line - begin])) {
                failLog(expected);
            }
        }
    }

    protected void failLog(String @NotNull [] expected) {
        failLog(expected, null);
    }

    protected void failLog(String @NotNull [] expected, @Nullable String message) {
        StringBuilder builder = new StringBuilder();
        builder.append("Expected log to contain:\n");
        for (String line : expected) {
            builder.append(line).append("\n");
        }
        builder.append("\n");
        builder.append("Actual log:\n");
        for (String line : log.writtenData) {
            builder.append(line).append("\n");
        }
        builder.append("\n");

        if (message != null) {
            fail(builder + message);
        } else {
            fail(builder.toString());
        }
    }

    /**
     * Create the tree starting from the root, with the names
     * Returns the last created DirectoryNode
     */
    protected void createTree(DirectoryNode root, int amountOfFiles,@Nullable CoverageMetrics defaultCoverage, @Nullable String @Nullable ... names) {
        if (names != null) {
            for (String name : names) {
                if (name == null) {
                    continue;
                }

                root = root.getSubdirectories().computeIfAbsent(name, DirectoryNode::new);
            }
        }

        // Adding debug files
        List<String> fileNames = new ArrayList<String>();
        for(int i = 0; i < amountOfFiles; i++) {
            fileNames.add("Example" + (fileCounter++) + ".java");
        }

        addFiles(root, defaultCoverage, fileNames.toArray(new String[0]));
    }

    protected void createTree(DirectoryNode root, int amountOfFiles, @Nullable String @Nullable ... names) {
        createTree(root, amountOfFiles, null, names);
    }

    protected void addFiles(DirectoryNode toNode, @Nullable String @Nullable ... names) {
        addFiles(toNode, null, names);
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
    protected @NotNull MavenProject createProjectWithJacocoPlugin(String destFile) {
        Model model = new Model();
        model.setGroupId("test.group");
        model.setArtifactId("test-artifact");
        model.setVersion("1.0.0");

        Build build = new Build();
        model.setBuild(build);

        Plugin plugin = new Plugin();
        plugin.setGroupId("org.jacoco");
        plugin.setArtifactId("jacoco-maven-plugin");
        plugin.setVersion("0.8.12");

        if (destFile != null) {
            Xpp3Dom configuration = new Xpp3Dom("configuration");
            Xpp3Dom destFileNode = new Xpp3Dom("destFile");
            destFileNode.setValue(destFile);
            configuration.addChild(destFileNode);
            plugin.setConfiguration(configuration);
        }

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
}
