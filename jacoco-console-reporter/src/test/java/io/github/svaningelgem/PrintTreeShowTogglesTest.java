package io.github.svaningelgem;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PrintTreeShowTogglesTest extends BaseTestClass {
    DirectoryNode root;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        CoverageMetrics metrics = new CoverageMetrics(8, 7, 6, 5, 4, 3, 2, 1);
        root = new DirectoryNode("");
        createTree(root, 1, metrics, "com", "example", "model");
        createTree(root, 1, metrics, "com", "example", "util");
        createTree(root, 0, metrics, "com", "example", "dummy"); // No files --> shouldn't show this!
    }

    @Test
    public void testTreeOnOthersOff() throws Exception {
        mojo.showTree = true;
        mojo.showFiles = false;

        mojo.printTree(root);

        String[] expected = {
                "[info] Overall Coverage Summary",
                "[info] Package                                            │ Class, %             │ Method, %            │ Branch, %            │ Line, %             ",
                "[info] ---------------------------------------------------│----------------------│----------------------│----------------------│---------------------",
                "[info] com.example                                        │ 87.50% (14/16)       │ 83.33% (10/12)       │ 50.00% (2/4)         │ 75.00% (6/8)        ",
                "[info] ├─model                                            │ 87.50% (7/8)         │ 83.33% (5/6)         │ 50.00% (1/2)         │ 75.00% (3/4)        ",
                "[info] └─util                                             │ 87.50% (7/8)         │ 83.33% (5/6)         │ 50.00% (1/2)         │ 75.00% (3/4)        ",
                "[info] ---------------------------------------------------│----------------------│----------------------│----------------------│---------------------",
                "[info] all classes                                        │ 87.50% (14/16)       │ 83.33% (10/12)       │ 50.00% (2/4)         │ 75.00% (6/8)        ",
        };

        assertLogContains(expected);
    }

    @Test
    public void testAllOn() throws Exception {
        mojo.showTree = true;
        mojo.showFiles = true;

        mojo.printTree(root);

        String[] expected = {
                "[info] Overall Coverage Summary",
                "[info] Package                                            │ Class, %             │ Method, %            │ Branch, %            │ Line, %             ",
                "[info] ---------------------------------------------------│----------------------│----------------------│----------------------│---------------------",
                "[info] com.example                                        │ 87.50% (14/16)       │ 83.33% (10/12)       │ 50.00% (2/4)         │ 75.00% (6/8)        ",
                "[info] ├─model                                            │ 87.50% (7/8)         │ 83.33% (5/6)         │ 50.00% (1/2)         │ 75.00% (3/4)        ",
                "[info] │ └─Example0.java                                  │ 87.50% (7/8)         │ 83.33% (5/6)         │ 50.00% (1/2)         │ 75.00% (3/4)        ",
                "[info] └─util                                             │ 87.50% (7/8)         │ 83.33% (5/6)         │ 50.00% (1/2)         │ 75.00% (3/4)        ",
                "[info]   └─Example1.java                                  │ 87.50% (7/8)         │ 83.33% (5/6)         │ 50.00% (1/2)         │ 75.00% (3/4)        ",
                "[info] ---------------------------------------------------│----------------------│----------------------│----------------------│---------------------",
                "[info] all classes                                        │ 87.50% (14/16)       │ 83.33% (10/12)       │ 50.00% (2/4)         │ 75.00% (6/8)        ",
        };

        assertLogContains(expected);
    }

    @Test
    public void testTreeOffFilesOn() throws Exception {
        mojo.showTree = false;
        mojo.showFiles = true;

        mojo.printTree(root);

        assertEquals(0, log.writtenData.size());
    }

    @Test
    public void testAllOff() throws Exception {
        mojo.showTree = false;
        mojo.showFiles = false;

        mojo.printTree(root);

        assertEquals(0, log.writtenData.size());
    }
}
