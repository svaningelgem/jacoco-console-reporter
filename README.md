# Updated Configuration Options section

## Configuration Options

While the plugin works with zero configuration, you can customize its behavior:

| Parameter | Description | Default Value |
|-----------|-------------|---------------|
| `deferReporting` | Wait until last module in multi-module builds | `true` |
| `scanModules` | Auto-discover exec files across modules | `false` |
| `showFiles` | Display individual source files in tree | `false` |
| `showMissingLines` | Display uncovered line numbers for each file (requires showFiles=true) | `false` |
| `showTree` | Display hierarchical package tree | `true` |
| `showSummary` | Display overall coverage summary | `true` |
| `ignoreFilesInBuildDirectory` | Auto-exclude generated files | `true` |
| `interpretSonarIgnorePatterns` | Apply Sonar exclusion patterns | `true` |
| `xmlOutputFile` | Generate aggregated XML report | `${session.executionRootDirectory}/coverage.xml` |

# Updated Sample Output section

## Sample Output

### Default Tree View
```text
[INFO] Overall Coverage Summary
[INFO] Package                                    │ Class, %         │ Method, %        │ Branch, %        │ Line, %
[INFO] ────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
[INFO] com.example                                │ 100.00% (3/3)    │ 83.33% (5/6)     │ 50.00% (2/4)     │ 75.00% (15/20)
[INFO] ├─model                                    │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)
[INFO] └─util                                     │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)
[INFO] ────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
[INFO] all classes                                │ 100.00% (3/3)    │ 83.33% (5/6)     │ 50.00% (2/4)     │ 75.00% (15/20)
```

### With File Details (`showFiles=true`)
```text
[INFO] com.example                                │ 100.00% (3/3)    │ 83.33% (5/6)     │ 50.00% (2/4)     │ 75.00% (15/20)
[INFO] ├─model                                    │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)
[INFO] │ └─Model.java                             │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)
[INFO] ├─util                                     │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)
[INFO] │ └─Util.java                              │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)
[INFO] └─Example.java                             │ 100.00% (1/1)    │ 33.33% (1/2)     │ 0.00% (0/0)      │ 25.00% (1/4)
```

### With Missing Lines (`showFiles=true` and `showMissingLines=true`)
```text
[INFO] com.example                                │ 100.00% (3/3)    │ 83.33% (5/6)     │ 50.00% (2/4)     │ 75.00% (15/20)
[INFO] ├─model                                    │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)
[INFO] │ └─Model.java                             │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)   Missing: 45
[INFO] ├─util                                     │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)
[INFO] │ └─Util.java                              │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)   Missing: 23
[INFO] └─Example.java                             │ 100.00% (1/1)    │ 33.33% (1/2)     │ 0.00% (0/0)      │ 25.00% (1/4)   Missing: 33-35, 39
```

The missing lines are displayed in a compact format:
- Individual uncovered lines: `23, 45, 67`
- Ranges of consecutive uncovered lines: `33-35`
- Combination: `23, 33-35, 39, 45-48`

# New Advanced Configuration Example

### Detailed Coverage with Missing Lines
```xml
<plugin>
    <groupId>io.github.svaningelgem</groupId>
    <artifactId>jacoco-console-reporter</artifactId>
    <version>1.0.0</version>
    <configuration>
        <!-- Show individual files and their uncovered lines -->
        <showFiles>true</showFiles>
        <showMissingLines>true</showMissingLines>
        <!-- Generate XML report as well -->
        <xmlOutputFile>${project.build.directory}/aggregated-coverage.xml</xmlOutputFile>
    </configuration>
</plugin>
```

This configuration provides maximum detail, showing exactly which lines in each file need additional test coverage, similar to Python's coverage.py output.