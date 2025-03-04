package io.github.svaningelgem;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RealLifeTest extends BaseTestClass {

    @Test
    public void testPrintTreeWithMultipleSubdirs() {

        // Create a complex case with both source files and multiple subdirectories
        DirectoryNode root = new DirectoryNode("");

        createTree(root, 2, "com", "example", "crim", "pro", "communication", "saml");
        createTree(root, 2, "com", "example", "crim", "pro", "communication", "saml", "ibm", "names", "it", "_1_0", "user");
        createTree(root, 2, "com", "example", "crim", "pro", "communication", "saml", "oasis", "names");
        createTree(root, 2, "com", "example", "crim", "pro", "controller");
        createTree(root, 2, "com", "example", "crim", "pro", "enums");
        createTree(root, 2, "com", "example", "crim", "pro", "externals", "employee", "v1", "models");
        createTree(root, 2, "com", "example", "crim", "pro", "externals", "employee", "v1", "services");
        createTree(root, 2, "com", "example", "archiving");

        // Enable showing files
        mojo.showFiles = false;

        // Print the tree
        root.printTree(mojo.getLog(), "", Defaults.LINE_FORMAT, "", mojo.showFiles);

        // Test output should match the expected format for com.example
        String[] expectedLines = {
                "com.example                 ",
                "├─archiving                 ",
                "└─crim.pro                  ",
                "  ├─communication.saml      ",
                "  │ ├─ibm.names.it._1_0.user",
                "  │ └─oasis.names           ",
                "  ├─controller              ",
                "  ├─enums                   ",
                "  └─externals.employee.v1   ",
                "    ├─models                ",
                "    └─services              "
        };
        assertLogContains(expectedLines);
    }

    @Test
    public void testPrintTreeWithMultipleSubdirsAndFiles() {

        // Create a complex case with both source files and multiple subdirectories
        DirectoryNode root = new DirectoryNode("");

        createTree(root, 2, "com", "example", "crim", "pro", "communication", "saml");
        createTree(root, 2, "com", "example", "crim", "pro", "communication", "saml", "ibm", "names", "it", "_1_0", "user");
        createTree(root, 2, "com", "example", "crim", "pro", "communication", "saml", "oasis", "names");
        createTree(root, 2, "com", "example", "crim", "pro", "controller");
        createTree(root, 2, "com", "example", "crim", "pro", "enums");
        createTree(root, 2, "com", "example", "crim", "pro", "externals", "employee", "v1", "models");
        createTree(root, 2, "com", "example", "crim", "pro", "externals", "employee", "v1", "services");
        createTree(root, 2, "com", "example", "archiving");

        // Enable showing files
        mojo.showFiles = true;

        // Print the tree
        root.printTree(mojo.getLog(), "", Defaults.LINE_FORMAT, "", mojo.showFiles);

        // Test output should match the expected format for com.example
        String[] expectedLines = {
                "com.example                 ",
                "├─archiving                 ",
                "│ ├─Example14.java          ",
                "│ └─Example15.java          ",
                "└─crim.pro                  ",
                "  ├─communication.saml      ",
                "  │ ├─ibm.names.it._1_0.user",
                "  │ │ ├─Example2.java       ",
                "  │ │ └─Example3.java       ",
                "  │ ├─oasis.names           ",
                "  │ │ ├─Example4.java       ",
                "  │ │ └─Example5.java       ",
                "  │ ├─Example0.java         ",
                "  │ └─Example1.java         ",
                "  ├─controller              ",
                "  │ ├─Example6.java         ",
                "  │ └─Example7.java         ",
                "  ├─enums                   ",
                "  │ ├─Example8.java         ",
                "  │ └─Example9.java         ",
                "  └─externals.employee.v1   ",
                "    ├─models                ",
                "    │ ├─Example10.java      ",
                "    │ └─Example11.java      ",
                "    └─services              ",
                "      ├─Example12.java      ",
                "      └─Example13.java      ",
        };
        assertLogContains(expectedLines);
    }
}
