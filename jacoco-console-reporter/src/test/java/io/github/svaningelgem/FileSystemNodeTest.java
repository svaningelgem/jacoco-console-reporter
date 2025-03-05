package io.github.svaningelgem;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileSystemNodeTest {

    @Test
    public void testDirectoryBeforeFile() {
        DirectoryNode dir = new DirectoryNode("dir");
        SourceFileNode file = new SourceFileNode("file", new CoverageMetrics());
        assertTrue(dir.compareTo(file) < 0);
    }

    @Test
    public void testFileAfterDirectory() {
        SourceFileNode file = new SourceFileNode("file", new CoverageMetrics());
        DirectoryNode dir = new DirectoryNode("dir");
        assertTrue(file.compareTo(dir) > 0);
    }

    @Test
    public void testDirectoriesAlphabetical() {
        DirectoryNode dir1 = new DirectoryNode("abc");
        DirectoryNode dir2 = new DirectoryNode("def");
        assertTrue(dir1.compareTo(dir2) < 0);
    }

    @Test
    public void testFilesAlphabetical() {
        SourceFileNode file1 = new SourceFileNode("abc", new CoverageMetrics());
        SourceFileNode file2 = new SourceFileNode("def", new CoverageMetrics());
        assertTrue(file1.compareTo(file2) < 0);
    }

    @Test
    public void testSameNameCaseInsensitive() {
        DirectoryNode dir1 = new DirectoryNode("Abc");
        DirectoryNode dir2 = new DirectoryNode("abc");
        assertEquals(0, dir1.compareTo(dir2));
    }
}
