# JaCoCo Console Reporter Maven Plugin

A custom Maven plugin that generates a textual tree-like coverage report from JaCoCo's execution data files, displaying coverage metrics (Class %, Method %, Branch %, Line %) for packages, source files, and the entire project.

## Features
- Reads coverage data from `jacoco.exec` files
- Analyzes class files from the project's build output directory
- Outputs a hierarchical console-based report with coverage metrics
- Tree-like package structure visualization with collapsible paths
- Instant visibility of coverage metrics during build
- Multi-module project support with option to defer reporting until the end
- Automatic scanning for `jacoco.exec` files across modules
- Support for custom JaCoCo execution file patterns
- Combined weighted coverage score based on customizable weights

## Prerequisites
- Maven 3.x
- JaCoCo plugin configured in your project to generate `jacoco.exec`

## Installation
Add the plugin to your project's pom.xml:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.svaningelgem</groupId>
            <artifactId>jacoco-console-reporter</artifactId>
            <version>1.0.0</version>
            <executions>
                <execution>
                    <goals>
                        <goal>report</goal>
                    </goals>
                    <phase>verify</phase>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## Usage
Run the plugin after tests:
```bash
mvn verify
```

Ensure the JaCoCo plugin has executed beforehand to generate jacoco.exec.

## Configuration

| Parameter             | Description                                               | Default Value                            |
|-----------------------|-----------------------------------------------------------|------------------------------------------|
| `jacocoExecFile`      | Path to the JaCoCo execution data file                    | `${project.build.directory}/jacoco.exec` |
| `classesDirectory`    | Directory containing compiled classes                     | `${project.build.outputDirectory}`       |
| `deferReporting`      | Defer reporting until the end (for multi-module projects) | `true`                                   |
| `showFiles`           | Whether to show individual source files in the report     | `false`                                  |
| `showTree`            | Whether to show the tree structure in the report          | `true`                                   |
| `showSummary`         | Whether to show the summary information                   | `true`                                   |
| `scanModules`         | Automatically scan for exec files in project modules      | `false`                                  |
| `baseDir`             | Base directory for module scanning                        | `${project.basedir}`                     |
| `additionalExecFiles` | Additional exec files to include in the report            | `[]`                                     |
| `weightClassCoverage` | Weight for class coverage in combined score               | `0.1`                                    |
| `weightMethodCoverage`| Weight for method coverage in combined score              | `0.1`                                    |
| `weightBranchCoverage`| Weight for branch coverage in combined score              | `0.4`                                    |
| `weightLineCoverage`  | Weight for line coverage in combined score                | `0.4`                                    |

## Default Output
```text
[INFO] Overall Coverage Summary
[INFO] Package                                    │ Class, %         │ Method, %        │ Branch, %        │ Line, %
[INFO] --------------------------------------------------------------------------------------------------------------------------------------------------------
[INFO] com.example                                │ 100.00% (3/3)    │ 83.33% (5/6)     │ 50.00% (2/4)     │ 75.00% (15/20)
[INFO] ├─model                                    │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)
[INFO] └─util                                     │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)
[INFO] --------------------------------------------------------------------------------------------------------------------------------------------------------
[INFO] all classes                                │ 100.00% (3/3)    │ 83.33% (5/6)     │ 50.00% (2/4)     │ 75.00% (15/20)
[INFO] Overall Coverage Summary
[INFO] ------------------------
[INFO] Class coverage : 100.00% (3/3)
[INFO] Method coverage: 83.33% (5/6)
[INFO] Branch coverage: 50.00% (2/4)
[INFO] Line coverage  : 75.00% (15/20)
[INFO] Combined coverage: 68.33% (Class 10%, Method 10%, Branch 40%, Line 40%)
```

## Output with all options on
```text
[INFO] Overall Coverage Summary
[INFO] Package                                    │ Class, %         │ Method, %        │ Branch, %        │ Line, %
[INFO] --------------------------------------------------------------------------------------------------------------------------------------------------------
[INFO] com.example                                │ 100.00% (3/3)    │ 83.33% (5/6)     │ 50.00% (2/4)     │ 75.00% (15/20)
[INFO] ├─model                                    │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)
[INFO] │ └─Model.java                             │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)
[INFO] ├─util                                     │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)
[INFO] │ └─Util.java                              │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)
[INFO] └─Example.java                             │ 100.00% (1/1)    │ 33.33% (1/2)     │ 0.00% (0/0)      │ 25.00% (1/4)
[INFO] --------------------------------------------------------------------------------------------------------------------------------------------------------
[INFO] all classes                                │ 100.00% (3/3)    │ 83.33% (5/6)     │ 50.00% (2/4)     │ 75.00% (15/20)
[INFO] Overall Coverage Summary
[INFO] ------------------------
[INFO] Class coverage : 100.00% (3/3)
[INFO] Method coverage: 83.33% (5/6)
[INFO] Branch coverage: 50.00% (2/4)
[INFO] Line coverage  : 75.00% (15/20)
[INFO] Combined coverage: 68.33% (Class 10%, Method 10%, Branch 40%, Line 40%)
```

## Advanced Usage

### Multi-Module Projects

The plugin is configured by default to defer reporting until the end.

This will wait with generating the report until the last module in the build.

### Custom JaCoCo File Locations

If your JaCoCo plugin uses a non-default location for the execution data file:

```xml
<plugin>
    <groupId>io.github.svaningelgem</groupId>
    <artifactId>jacoco-console-reporter</artifactId>
    <version>1.0.0</version>
    <configuration>
        <jacocoExecFile>${project.build.directory}/custom-jacoco.exec</jacocoExecFile>
    </configuration>
    <!-- ... -->
</plugin>
```

### Customizing Report Output

You can configure which parts of the report are displayed:

```xml
<plugin>
    <groupId>io.github.svaningelgem</groupId>
    <artifactId>jacoco-console-reporter</artifactId>
    <version>1.0.0</version>
    <configuration>
        <!-- Show or hide the tree structure -->
        <showTree>true</showTree>
        <!-- Show or hide individual source files -->
        <showFiles>true</showFiles>
        <!-- Show or hide the summary information -->
        <showSummary>true</showSummary>
    </configuration>
    <!-- ... -->
</plugin>
```

### Customizing Coverage Weights

You can adjust the weights used to calculate the combined coverage score:

```xml
<plugin>
    <groupId>io.github.svaningelgem</groupId>
    <artifactId>jacoco-console-reporter</artifactId>
    <version>1.0.0</version>
    <configuration>
        <weightClassCoverage>0.2</weightClassCoverage>
        <weightMethodCoverage>0.2</weightMethodCoverage>
        <weightBranchCoverage>0.3</weightBranchCoverage>
        <weightLineCoverage>0.3</weightLineCoverage>
    </configuration>
    <!-- ... -->
</plugin>
```

## Implementation Details

The plugin works by:

1. Detecting JaCoCo execution data files (default or custom locations)
2. Loading the execution data using JaCoCo's API
3. Analyzing compiled classes using the execution data
4. Building a hierarchical directory structure representing the package organization
5. Calculating coverage metrics (class, method, branch, line) for each node
6. Generating a tree-like report to the console
7. Computing a weighted combined coverage score

## Contributing

Contributions are welcome! Feel free to submit issues or pull requests to enhance this plugin.

### Building from Source

```bash
mvn clean install
```

### Running Tests

```bash
mvn test
```