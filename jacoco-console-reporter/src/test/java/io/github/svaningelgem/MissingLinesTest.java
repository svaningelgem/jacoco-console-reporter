package io.github.svaningelgem;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MissingLinesTest extends BaseTestClass {
    @Test
    public void testPrintTreeWithMissingLines() {
        mojo.showFiles = true;
        mojo.showMissingLines = true;
        mojo.showTree = true;

        DirectoryNode root = new DirectoryNode("");
        DirectoryNode comNode = root.getSubdirectories().computeIfAbsent("com", DirectoryNode::new);
        DirectoryNode exampleNode = comNode.getSubdirectories().computeIfAbsent("example", DirectoryNode::new);

        CoverageMetrics metrics = new CoverageMetrics(1, 1, 5, 4, 10, 7, 4, 3);
        exampleNode.getSourceFiles().add(new SourceFileNode("Test.java", metrics, "3-5, 10; partial: 15"));

        mojo.printTree(root);

        boolean foundMissingLines = log.writtenData.stream()
                .anyMatch(line -> line.contains("Missing: 3-5, 10; partial: 15"));
        assertTrue("Should display missing lines in output", foundMissingLines);
    }

    @Test
    public void testSourceFileNodePrintTreeWithMissingLines() {
        SourceFileNode node = new SourceFileNode("Test.java", new CoverageMetrics(1, 1, 5, 4, 10, 7, 4, 3), "3-5, 10");

        node.printTree(log, "├─", Defaults.getInstance().lineFormat, "", true);

        boolean foundMissingLines = log.writtenData.stream()
                .anyMatch(line -> line.contains("Missing: 3-5, 10"));
        assertTrue("Should display missing lines", foundMissingLines);
    }

    @Test
    public void testSourceFileNodePrintTreeWithoutMissingLines() {
        SourceFileNode node = new SourceFileNode("Test.java", new CoverageMetrics(1, 1, 5, 4, 10, 8, 4, 4), null);

        node.printTree(log, "├─", Defaults.getInstance().lineFormat, "", true);

        boolean foundMissingLines = log.writtenData.stream()
                .anyMatch(line -> line.contains("Missing:"));
        assertFalse("Should not display missing lines when null", foundMissingLines);
    }

    @Test
    public void testSourceFileNodePrintTreeWithEmptyMissingLines() {
        SourceFileNode node = new SourceFileNode("Test.java", new CoverageMetrics(1, 1, 5, 5, 10, 10, 4, 4), "");

        node.printTree(log, "├─", Defaults.getInstance().lineFormat, "", true);

        boolean foundMissingLines = log.writtenData.stream()
                .anyMatch(line -> line.contains("Missing:"));
        assertFalse("Should not display missing lines when empty", foundMissingLines);
    }
}
