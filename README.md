# Jacoco Console Reporter Maven Plugin

A custom Maven plugin that generates a textual tabular coverage report from JaCoCo's `jacoco.exec` file, displaying coverage metrics (Class %, Method %, Branch %, Line %) for packages, source files, and the entire project.

## Features
- Reads coverage data from `jacoco.exec`.
- Analyzes class files from the project's build output directory.
- Outputs a console-based tabular report, e.g.:
```text
  Overall Coverage Summary
  Package | Class, % | Method, % | Branch, % | Line, %
  com.example | 50.00% (1/2) | 33.33% (2/6) | 25.00% (1/4) | 40.00% (4/10)
  all classes | 30.40% (28/92) | 18.30% (100/545) | 21.00% (274/1302) | 21.90% (494/2251)
```

## Prerequisites
- Maven 3.x
- JaCoCo plugin configured in your project to generate `jacoco.exec`.

## Installation
1. Clone this repository:
```bash
 git clone <repository-url>
```
2. Build the plugin:

```bash
    mvn clean install
```
3. Add the plugin to your project's pom.xml:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.example</groupId>
            <artifactId>jacoco-console-reporter</artifactId>
            <version>1.0-SNAPSHOT</version>
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

* `jacocoExecFile`: Path to `jacoco.exec` (default: `${project.build.directory}/jacoco.exec`).
* `classesDirectory`: Directory containing compiled classes (default: `${project.build.outputDirectory}`).

## Example Output
```text
[INFO] Overall Coverage Summary
[INFO] Package | Class, % | Method, % | Branch, % | Line, %
[INFO] com.example | 50.00% (1/2) | 33.33% (2/6) | 25.00% (1/4) | 40.00% (4/10)
[INFO] File: Example.java | 50.00% (1/2) | 33.33% (2/6) | 25.00% (1/4) | 40.00% (4/10)
[INFO] all classes | 50.00% (1/2) | 33.33% (2/6) | 25.00% (1/4) | 40.00% (4/10)
```

## Contributing
Feel free to submit issues or pull requests to enhance this plugin!
