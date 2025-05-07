package io.github.svaningelgem;

import org.apache.maven.plugin.logging.Log;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class DirectoryNodeTest extends BaseTestClass {

    @Test
    public void testShouldInclude() {
        DirectoryNode emptyNode = new DirectoryNode("empty");
        assertFalse("Empty directory should not be included", emptyNode.shouldInclude());

        DirectoryNode withFiles = new DirectoryNode("withFiles");
        withFiles.getSourceFiles().add(new SourceFileNode("Test.java", new CoverageMetrics()));
        assertTrue("Directory with files should be included", withFiles.shouldInclude());

        DirectoryNode withSubDir = new DirectoryNode("withSubDir");
        DirectoryNode subDir = new DirectoryNode("subDir");
        subDir.getSourceFiles().add(new SourceFileNode("Test.java", new CoverageMetrics()));
        withSubDir.getSubdirectories().put("subDir", subDir);
        assertTrue("Directory with non-empty subdirectory should be included", withSubDir.shouldInclude());

        DirectoryNode withEmptySubDir = new DirectoryNode("withEmptySubDir");
        withEmptySubDir.getSubdirectories().put("emptySubDir", new DirectoryNode("emptySubDir"));
        assertFalse("Directory with only empty subdirectory should not be included", withEmptySubDir.shouldInclude());
    }

    @Test
    public void testCollapseSingleNodeDirectory() {
        DirectoryNode root = new DirectoryNode("");
        DirectoryNode singleChild = new DirectoryNode("single");
        root.getSubdirectories().put("single", singleChild);

        // This will test the shouldCollapse condition: dirNodes.size() == 1 && fileNodes.isEmpty()
        root.printTree(log, "", Defaults.getInstance().lineFormat, "", false);

        // The log should only contain the collapsed path, not separate entries for root and single
        assertFalse("Log should not contain the root node separately",
                log.writtenData.stream().anyMatch(s -> s.contains("<root>")));
    }

    @Test
    public void testNoCollapseWithMultipleDirectories() {
        DirectoryNode root = new DirectoryNode("");
        root.getSubdirectories().put("dir1", new DirectoryNode("dir1"));
        root.getSubdirectories().put("dir2", new DirectoryNode("dir2"));

        // Add a file to each directory so shouldInclude() returns true
        root.getSubdirectories().get("dir1").getSourceFiles().add(
                new SourceFileNode("Test1.java", new CoverageMetrics()));
        root.getSubdirectories().get("dir2").getSourceFiles().add(
                new SourceFileNode("Test2.java", new CoverageMetrics()));

        // This will test the !shouldCollapse condition
        root.printTree(log, "", Defaults.getInstance().lineFormat, "", false);

        // The log should contain the root and both directories as separate entries
        assertTrue("Log should contain the root node",
                log.writtenData.stream().anyMatch(s -> s.contains("<root>")));
    }

    @Test
    public void testNoCollapseWithFiles() {
        DirectoryNode root = new DirectoryNode("");
        root.getSubdirectories().put("dir", new DirectoryNode("dir"));
        root.getSourceFiles().add(new SourceFileNode("RootTest.java", new CoverageMetrics()));

        // Add a file to the directory so shouldInclude() returns true
        root.getSubdirectories().get("dir").getSourceFiles().add(
                new SourceFileNode("Test.java", new CoverageMetrics()));

        // This will test the !shouldCollapse condition due to files in root
        root.printTree(log, "", Defaults.getInstance().lineFormat, "", true);

        // The log should contain the root and directory as separate entries
        assertTrue("Log should contain the root node",
                log.writtenData.stream().anyMatch(s -> s.contains("<root>")));
    }

    @Test
    public void testPrintNodesWithEmptyList() {
        DirectoryNode root = new DirectoryNode("");

        // Access the private method using reflection
        try {
            java.lang.reflect.Method printNodesMethod = DirectoryNode.class.getDeclaredMethod(
                    "printNodes",
                    Log.class,
                    String.class,
                    String.class,
                    String.class,
                    boolean.class,
                    List.class,
                    boolean.class
            );
            printNodesMethod.setAccessible(true);

            // Call the method with an empty list - should not throw exception
            printNodesMethod.invoke(root, log, "", "", "", true, Collections.emptyList(), true);

            // Test passed if no exception was thrown
            assertTrue(true);
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }

    @Test
    public void testDetermineNewPrefixCases() {
        DirectoryNode node = new DirectoryNode("test");

        try {
            java.lang.reflect.Method determineNewPrefixMethod = DirectoryNode.class.getDeclaredMethod(
                    "determineNewPrefix",
                    String.class,
                    boolean.class
            );
            determineNewPrefixMethod.setAccessible(true);

            // Test with corner prefix
            String result1 = (String) determineNewPrefixMethod.invoke(node, Defaults.getInstance().corner, true);
            assertEquals("Should replace corner with space and add corner",
                    Defaults.getInstance().lastDirSpace + Defaults.getInstance().corner, result1);

            // Test with tee prefix
            String result2 = (String) determineNewPrefixMethod.invoke(node, Defaults.getInstance().tee, false);
            assertEquals("Should replace tee with vertical line and add tee",
                    Defaults.getInstance().verticalLine + Defaults.getInstance().tee, result2);

            // Test with other prefix not ending in corner or tee
            String otherPrefix = "  ";
            String result3 = (String) determineNewPrefixMethod.invoke(node, otherPrefix, true);
            assertEquals("Should add corner to prefix without modification",
                    otherPrefix + Defaults.getInstance().corner, result3);

        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }
}