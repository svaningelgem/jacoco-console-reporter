package io.github.svaningelgem;

import org.apache.maven.plugin.logging.SystemStreamLog;

import java.util.ArrayList;
import java.util.List;

public class MyLog extends SystemStreamLog {
    public List<String> writtenData = new ArrayList<>();

    @Override
    public void info(CharSequence content) {
        writtenData.add(content.toString());
        super.info(content);
    }
}
