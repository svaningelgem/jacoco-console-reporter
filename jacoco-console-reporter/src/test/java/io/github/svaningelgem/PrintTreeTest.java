package io.github.svaningelgem;

import org.junit.Test;

public class PrintTreeTest extends BaseTestClass {
    @Test
    public void testPrintTreeWithShowFilesEnabled() {
        // Create a sample directory structure with files
        DirectoryNode root = new DirectoryNode("");
        createTree(root, 1, "pkg");

        // Set showFiles to true
        mojo.showFiles = true;

        // Print the tree directly
        root.printTree(mojo.getLog(), "", Defaults.getInstance().lineFormat, "", true);

        String[] expected = {
                "[info] pkg            ",
                "[info] └─Example0.java",
        };
        assertLogContains(expected);
    }

    @Test
    public void testMultipleSubdirectoriesCase() {
        // Create a sample directory structure with multiple subdirectories
        DirectoryNode root = new DirectoryNode("");
        createTree(root, 1, "pkg1");
        createTree(root, 1, "pkg2");

        // Print the tree
        root.printTree(mojo.getLog(), "", Defaults.getInstance().lineFormat, "", true);

        String[] expected = {
                "[info] <root>           ",
                "[info] ├─pkg1           ",
                "[info] │ └─Example0.java",
                "[info] └─pkg2           ",
                "[info]   └─Example1.java",
        };
        assertLogContains(expected);
    }

    @Test
    public void testPrintTreeWithNonEmptySubdirectory() {
        // Create a complex directory structure
        // root -> com -> example -> (file1.java, file2.java)
        DirectoryNode root = new DirectoryNode("");
        createTree(root, 2, "com", "example");

        // Print the tree
        root.printTree(mojo.getLog(), "", Defaults.getInstance().lineFormat, "", true);

        String[] expected = {
                "[info] com.example      ",
                "[info] ├─Example0.java  ",
                "[info] └─Example1.java  "
        };
        assertLogContains(expected);
    }

    @Test
    public void testPrintTreeWithCollapsedDirectoryPath() {
        // Create a directory structure perfect for collapsing
        // com -> example -> util -> (Util.java)
        DirectoryNode root = new DirectoryNode("");
        createTree(root, 1, "com", "example", "util");

        // Print the tree - should collapse the path
        root.printTree(mojo.getLog(), "", Defaults.getInstance().lineFormat, "", true);

        String[] expected = {
                "[info] com.example.util ",
                "[info] └─Example0.java  "
        };
        assertLogContains(expected);
    }

    @Test
    public void testPrintTreeWithSourceFilesAndMultipleSubdirs() {
        // Create a complex case with both source files and multiple subdirectories
        DirectoryNode root = new DirectoryNode("");

        createTree(root, 1, "com", "example", "model");
        createTree(root, 1, "com", "example", "util");
        createTree(root, 1, "com", "example");

        // Enable showing files
        mojo.showFiles = true;

        // Print the tree
        root.printTree(mojo.getLog(), "", Defaults.getInstance().lineFormat, "", true);

        // Test output should match the expected format for com.example
        String[] expected = {
                "[info] com.example       ",
                "[info] ├─model           ",
                "[info] │ └─Example0.java ",
                "[info] ├─util            ",
                "[info] │ └─Example1.java ",
                "[info] └─Example2.java   "
        };
        assertLogContains(expected);
    }

    @Test
    public void testPrintTreeWithSourceFilesAndMultipleSubdirs2() {
        // Create a complex case with both source files and multiple subdirectories
        DirectoryNode root = new DirectoryNode("");
        createTree(root, 1, "com", "example", "model");
        createTree(root, 1, "com", "example", "util");
        createTree(root, 0, "com", "example", "dummy"); // No files --> shouldn't show this!

        // Enable showing files
        mojo.showFiles = true;

        // Print the tree
        root.printTree(mojo.getLog(), "", Defaults.getInstance().lineFormat, "", true);

        String[] expected = {
                "[info] com.example       ",
                "[info] ├─model           ",
                "[info] │ └─Example0.java ",
                "[info] └─util            ",
                "[info]   └─Example1.java ",
        };

        assertLogContains(expected);
    }
}
