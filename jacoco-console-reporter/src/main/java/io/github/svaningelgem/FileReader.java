package io.github.svaningelgem;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileReader {
    public String readAllBytes(Path path) throws IOException {
        return readAllBytes(path, StandardCharsets.UTF_8);
    }

    public String readAllBytes(Path path, Charset cs) throws IOException {
        return new String(Files.readAllBytes(path), cs);
    }

    public String canonicalPath(@NotNull File f) throws IOException {
        return f.getCanonicalPath();
    }
}
