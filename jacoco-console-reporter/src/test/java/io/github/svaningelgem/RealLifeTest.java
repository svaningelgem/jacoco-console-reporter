package io.github.svaningelgem;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RealLifeTest extends BaseTestClass {

    @Test
    public void testPrintTreeWithSourceFilesAndMultipleSubdirs() {

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
            "[info] com.example                 ",
            "[info] ├─archiving                 ",
            "[info] └─crim.pro                  ",
            "[info]   ├─communication.saml      ",
            "[info]   │ ├─ibm.names.it._1_0.user",
            "[info]   │ └─oasis.names           ",
            "[info]   ├─controller              ",
            "[info]   ├─enums                   ",
            "[info]   └─externals.employee.v1   ",
            "[info]     ├─models                ",
            "[info]     └─services              "
        };
        assertLogContains(expectedLines);
    }
}
