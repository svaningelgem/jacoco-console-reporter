package io.github.svaningelgem;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MyLog extends SystemStreamLog {
    public List<String> writtenData = new ArrayList<>();

    @Override
    public void info(@NotNull CharSequence content) {
        writtenData.add("[info] " + content);
    }

    @Override
    public void warn(@NotNull CharSequence content) {
        writtenData.add("[warn] " + content);
    }

    @Override
    public void warn(@NotNull CharSequence content, Throwable e) {
        writtenData.add("[warn] " + content);
    }

    @Override
    public void debug(@NotNull CharSequence content) {
        writtenData.add("[debug] " + content);
    }
}
