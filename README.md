# Jacoco Console Reporter Maven Plugin

A custom Maven plugin that generates a textual tree-like coverage report from JaCoCo's `jacoco.exec` file, displaying coverage metrics (Class %, Method %, Branch %, Line %) for packages, source files, and the entire project.

## Features
- Reads coverage data from `jacoco.exec`
- Analyzes class files from the project's build output directory
- Outputs a hierarchical console-based report with coverage metrics
- Tree-like package structure visualization
- Instant visibility of coverage metrics during build

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

* `jacocoExecFile`: Path to `jacoco.exec` (default: `${project.build.directory}/jacoco.exec`)
* `classesDirectory`: Directory containing compiled classes (default: `${project.build.outputDirectory}`)

## Example Output
```text
[INFO] Overall Coverage Summary
[INFO] Package                                    | Class, %          | Method, %         | Branch, %         | Line, %
[INFO] --------------------------------------------------------------------------------------------------------------------------------------------------------
[INFO] com.example                                | 50.00% (1/2)     | 33.33% (2/6)     | 25.00% (1/4)     | 40.00% (4/10)
[INFO]   ├─ Calculator.java                       | 50.00% (1/2)     | 50.00% (1/2)     | 0.00% (0/0)      | 50.00% (1/2)
[INFO]   └─ AdvancedCalculator.java              | 100.00% (1/1)    | 25.00% (1/4)     | 25.00% (1/4)     | 37.50% (3/8)
[INFO] --------------------------------------------------------------------------------------------------------------------------------------------------------
[INFO] all classes                                | 75.00% (2/3)     | 33.33% (2/6)     | 25.00% (1/4)     | 40.00% (4/10)
```

## Contributing
Contributions are welcome! Feel free to submit issues or pull requests to enhance this plugin.