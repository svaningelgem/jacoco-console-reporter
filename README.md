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
| `deferReporting`      | Defer reporting until the end (for multi-module projects) | `false`                                  |
| `showFiles`           | Whether to show individual source files in the report     | `true`                                   |
| `scanModules`         | Automatically scan for exec files in project modules      | `false`                                  |
| `baseDir`             | Base directory for module scanning                        | `${project.basedir}`                     |
| `additionalExecFiles` | Additional exec files to include in the report            | `[]`                                     |

## Example Output
```text
[INFO] Overall Coverage Summary
[INFO] Package                                    | Class, %          | Method, %         | Branch, %         | Line, %
[INFO] --------------------------------------------------------------------------------------------------------------------------------------------------------
[INFO] com.example                                | 100.00% (3/3)    | 83.33% (5/6)     | 50.00% (2/4)     | 75.00% (15/20)
[INFO] ├─model                                    | 100.00% (1/1)    | 100.00% (2/2)    | 50.00% (1/2)     | 87.50% (7/8)
[INFO] │ └─Model.java                             | 100.00% (1/1)    | 100.00% (2/2)    | 50.00% (1/2)     | 87.50% (7/8)
[INFO] ├─util                                     | 100.00% (1/1)    | 100.00% (2/2)    | 50.00% (1/2)     | 87.50% (7/8)
[INFO] │ └─Util.java                              | 100.00% (1/1)    | 100.00% (2/2)    | 50.00% (1/2)     | 87.50% (7/8)
[INFO] └─Example.java                             | 100.00% (1/1)    | 33.33% (1/2)     | 0.00% (0/0)      | 25.00% (1/4)
[INFO] --------------------------------------------------------------------------------------------------------------------------------------------------------
[INFO] all classes                                | 100.00% (3/3)    | 83.33% (5/6)     | 50.00% (2/4)     | 75.00% (15/20)
```

## Advanced Usage

### Multi-Module Projects

For multi-module projects, you can configure the plugin to aggregate coverage across modules. All configuration values are the defaults so they don't have to be added, but are shown here for completeness’s sake:

```xml
<plugin>
    <groupId>io.github.svaningelgem</groupId>
    <artifactId>jacoco-console-reporter</artifactId>
    <version>1.0.0</version>
    <configuration>
        <jacocoExecFile>${project.build.directory}/jacoco.exec</jacocoExecFile>
        <classesDirectory>${project.build.outputDirectory}</classesDirectory>
        <deferReporting>true</deferReporting>
        <showFiles>true</showFiles>
        <additionalExecFiles />
        <scanModules>false</scanModules>
        <baseDir>${project.basedir}</baseDir>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>report</goal>
            </goals>
            <phase>verify</phase>
        </execution>
    </executions>
</plugin>
```

This configuration will:
- Defer the reporting until the last module in the build
- Automatically scan for JaCoCo execution files in all modules

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

## Implementation Details

The plugin works by:

1. Detecting JaCoCo execution data files (default or custom locations)
2. Loading the execution data using JaCoCo's API
3. Analyzing compiled classes using the execution data
4. Building a hierarchical directory structure representing the package organization
5. Calculating coverage metrics (class, method, branch, line) for each node
6. Generating a tree-like report to the console

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