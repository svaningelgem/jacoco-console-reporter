package io.github.svaningelgem;

import lombok.var;
import org.apache.maven.plugin.testing.MojoRule;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static org.junit.Assert.*;

public class BaseTestClass {
    protected final static Random RANDOM = new Random();

    @Rule
    public final MojoRule rule = new MojoRule();

    protected JacocoConsoleReporterMojo mojo;
    protected MyLog log;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected final File testProjectDir = new File("../test-project");
    protected final File testProjectJacocoExec = new File(testProjectDir, "target/jacoco.exec");
    protected final File testProjectClasses = new File(testProjectDir, "target/classes");
    protected final File pom = new File(getBasedir(), "src/test/resources/unit/pom.xml");

    private int fileCounter = 0;

    protected String getBasedir() {
        return System.getProperty("basedir", new File("").getAbsolutePath());
    }

    @Before
    public void setUp() throws Exception {
        fileCounter = 0;

        mojo = (JacocoConsoleReporterMojo) rule.lookupConfiguredMojo(pom.getParentFile(), "report");
        log = new MyLog();
        mojo.setLog(log);
    }

    private static int nextInt(int bound) {
        if (bound == 0) return 0;

        return RANDOM.nextInt(bound);
    }

    @Contract(" -> new")
    protected static @NotNull CoverageMetrics getRandomCoverage() {
        var cm = new CoverageMetrics();

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
    protected DirectoryNode createTree(DirectoryNode root, int amountOfFiles, @Nullable String @Nullable ... names) {
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

        addFiles(root, fileNames.toArray(new String[0]));
        // And get back our test object
        return root;
    }

    protected void addFiles(DirectoryNode toNode, @Nullable String @Nullable ... names) {
        if (names == null) {
            return;
        }

        for (String name : names) {
            if (name == null) {
                continue;
            }

            var file = new SourceFileNode(name, getRandomCoverage());
            toNode.getSourceFiles().add(file);
        }
    }
}
