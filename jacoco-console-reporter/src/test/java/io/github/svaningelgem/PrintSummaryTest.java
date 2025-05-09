package io.github.svaningelgem;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class PrintSummaryTest extends BaseTestClass {
    DirectoryNode root;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        mojo.showSummary = false;

        mojo.weightClassCoverage = 0.1;
        mojo.weightMethodCoverage = 0.1;
        mojo.weightLineCoverage = 0.4;
        mojo.weightBranchCoverage = 0.4;

        CoverageMetrics metrics = new CoverageMetrics(8,7,6,5,4,3,2,1);
        root = new DirectoryNode("");
        createTree(root, 1, metrics, "com", "example", "model");
        createTree(root, 1, metrics, "com", "example", "util");
        createTree(root, 0, metrics, "com", "example", "dummy"); // No files --> shouldn't show this!
    }

    @Test
    public void testDifferentWeight() throws Exception {
        mojo.showSummary = true;

        mojo.weightClassCoverage = 0.25;
        mojo.weightMethodCoverage = 0.25;
        mojo.weightLineCoverage = 0.25;
        mojo.weightBranchCoverage = 0.25;

        mojo.printSummary(root);

        String[] expected = {
                "[info] Overall Coverage Summary",
                "[info] ------------------------",
                "[info] Class coverage : 87.50% (14/16)",
                "[info] Method coverage: 83.33% (10/12)",
                "[info] Branch coverage: 50.00% (2/4)",
                "[info] Line coverage  : 75.00% (6/8)",
                "[info] Combined coverage: 80.00% (Class 25%, Method 25%, Branch 25%, Line 25%)",
        };

        assertEquals(Arrays.asList(expected), log.writtenData);
    }

    @Test
    public void testSummaryOn() throws Exception {
        mojo.showSummary = true;

        mojo.printSummary(root);

        String[] expected = {
                "[info] Overall Coverage Summary",
                "[info] ------------------------",
                "[info] Class coverage : 87.50% (14/16)",
                "[info] Method coverage: 83.33% (10/12)",
                "[info] Branch coverage: 50.00% (2/4)",
                "[info] Line coverage  : 75.00% (6/8)",
                "[info] Combined coverage: 73.68% (Class 10%, Method 10%, Branch 40%, Line 40%)",
        };

        assertEquals(Arrays.asList(expected), log.writtenData);
    }

    @Test
    public void testSummaryOff() throws Exception {
        mojo.printSummary(root);

        assertEquals(0, log.writtenData.size());
    }
}
