# JaCoCo Console Reporter Maven Plugin

A smart Maven plugin that generates textual tree-like coverage reports from JaCoCo execution data with intelligent auto-detection of project configurations. Displays coverage metrics (Class %, Method %, Branch %, Line %) for packages, source files, and the entire project with minimal configuration required.

## Key Features

### Intelligent Auto-Detection
- **JaCoCo Plugin Integration**: Automatically detects and applies exclusion patterns from your existing JaCoCo plugin configuration
- **Sonar Pattern Support**: Reads and interprets `sonar.exclusions` and `sonar.coverage.exclusions` from project properties
- **Build Directory Exclusion**: Automatically excludes auto-generated files by parsing package declarations in the build directory
- **Multi-Module Discovery**: Automatically scans for `jacoco.exec` files across all project modules
- **Charset Detection**: Automatically detects console encoding (UTF-8 vs ASCII) for proper tree character rendering

### Smart Defaults
- **Deferred Reporting**: Waits until the last module in multi-module builds to provide aggregated coverage
- **Execution Data Deduplication**: Prevents double-counting of coverage when aggregating multiple modules
- **Zero Configuration**: Works out-of-the-box with standard Maven/JaCoCo setups

### Flexible Output Options
- Tree-like package structure visualization with collapsible paths
- Individual source file metrics (optional)
- Combined weighted coverage score with customizable weights
- Optional aggregated JaCoCo XML report generation
- Console-optimized formatting with proper Unicode/ASCII fallback

## Prerequisites

- Maven 3.x
- JaCoCo plugin configured in your project (the console reporter will auto-detect its configuration)

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

The plugin automatically detects your JaCoCo configuration and generates a console report with no additional setup required.

## How Auto-Detection Works

### JaCoCo Plugin Integration
The plugin automatically scans your project's JaCoCo plugin configuration to extract:
- **Exclusion patterns** from `<excludes><exclude>` elements
- **Custom exec file locations** from `<destFile>` configurations
- **Multiple execution configurations** across different plugin executions

### Sonar Integration
Reads exclusion patterns from project properties:
```xml
<properties>
    <sonar.exclusions>src/main/java/com/example/generated/**,**/*Generated.java</sonar.exclusions>
    <sonar.coverage.exclusions>src/test/java/**,**/*Test.java</sonar.coverage.exclusions>
</properties>
```

### Build Directory Analysis
Automatically excludes generated files by:
1. Scanning the `target` directory for `.java` files
2. Parsing package declarations to determine class names
3. Creating exclusion patterns for the corresponding `.class` files

This prevents artificially low coverage scores caused by generated code.

## Configuration Options

While the plugin works with zero configuration, you can customize its behavior:

| Parameter | Description | Default Value |
|-----------|-------------|---------------|
| `deferReporting` | Wait until last module in multi-module builds | `true` |
| `scanModules` | Auto-discover exec files across modules | `false` |
| `showFiles` | Display individual source files in tree | `false` |
| `showTree` | Display hierarchical package tree | `true` |
| `showSummary` | Display overall coverage summary | `true` |
| `ignoreFilesInBuildDirectory` | Auto-exclude generated files | `true` |
| `interpretSonarIgnorePatterns` | Apply Sonar exclusion patterns | `true` |
| `xmlOutputFile` | Generate aggregated XML report | `${session.executionRootDirectory}/coverage.xml` |

### Coverage Weight Customization
Control how the combined coverage score is calculated:

| Parameter | Description | Default |
|-----------|-------------|---------|
| `weightClassCoverage` | Weight for class coverage | `0.1` |
| `weightMethodCoverage` | Weight for method coverage | `0.1` |
| `weightBranchCoverage` | Weight for branch coverage | `0.4` |
| `weightLineCoverage` | Weight for line coverage | `0.4` |

## Sample Output

### Default Tree View
```text
[INFO] Overall Coverage Summary
[INFO] Package                                    │ Class, %         │ Method, %        │ Branch, %        │ Line, %
[INFO] ────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
[INFO] com.example                                │ 100.00% (3/3)    │ 83.33% (5/6)     │ 50.00% (2/4)     │ 75.00% (15/20)
[INFO] ├─model                                    │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)
[INFO] └─util                                     │ 100.00% (1/1)    │ 100.00% (2/2)    │ 50.00% (1/2)     │ 87.50% (7/8)
[INFO] ────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
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

### Summary Section
```text
[INFO] Overall Coverage Summary
[INFO] ------------------------
[INFO] Class coverage : 100.00% (3/3)
[INFO] Method coverage: 83.33% (5/6)
[INFO] Branch coverage: 50.00% (2/4)
[INFO] Line coverage  : 75.00% (15/20)
[INFO] Combined coverage: 68.33% (Class 10%, Method 10%, Branch 40%, Line 40%)
```

## Advanced Configuration Examples

### Multi-Module with Custom Weights
```xml
<plugin>
    <groupId>io.github.svaningelgem</groupId>
    <artifactId>jacoco-console-reporter</artifactId>
    <version>1.0.0</version>
    <configuration>
        <!-- Enable module scanning for automatic exec file discovery -->
        <scanModules>true</scanModules>
        <!-- Emphasize line and branch coverage over class/method -->
        <weightClassCoverage>0.05</weightClassCoverage>
        <weightMethodCoverage>0.15</weightMethodCoverage>
        <weightBranchCoverage>0.4</weightBranchCoverage>
        <weightLineCoverage>0.4</weightLineCoverage>
    </configuration>
</plugin>
```

### Detailed File-Level Reporting
```xml
<plugin>
    <groupId>io.github.svaningelgem</groupId>
    <artifactId>jacoco-console-reporter</artifactId>
    <version>1.0.0</version>
    <configuration>
        <showFiles>true</showFiles>
        <xmlOutputFile>${project.build.directory}/aggregated-coverage.xml</xmlOutputFile>
    </configuration>
</plugin>
```

### Disable Auto-Detection Features
```xml
<plugin>
    <groupId>io.github.svaningelgem</groupId>
    <artifactId>jacoco-console-reporter</artifactId>
    <version>1.0.0</version>
    <configuration>
        <!-- Disable automatic exclusions if you want full control -->
        <ignoreFilesInBuildDirectory>false</ignoreFilesInBuildDirectory>
        <interpretSonarIgnorePatterns>false</interpretSonarIgnorePatterns>
    </configuration>
</plugin>
```

## Integration with Existing Tools

### JaCoCo Plugin Compatibility
The console reporter automatically respects your existing JaCoCo configuration:

```xml
<!-- Your existing JaCoCo setup -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>com/example/generated/**/*</exclude>
            <exclude>**/*DTO.class</exclude>
        </excludes>
        <destFile>${project.build.directory}/custom-jacoco.exec</destFile>
    </configuration>
</plugin>

<!-- Console reporter automatically detects the above configuration -->
<plugin>
    <groupId>io.github.svaningelgem</groupId>
    <artifactId>jacoco-console-reporter</artifactId>
    <version>1.0.0</version>
    <!-- No additional configuration needed -->
</plugin>
```

### Sonar Integration
Works seamlessly with existing Sonar configurations:

```xml
<properties>
    <!-- Console reporter automatically applies these patterns -->
    <sonar.exclusions>
        src/main/java/com/example/generated/**,
        **/*Generated.java,
        **/target/**
    </sonar.exclusions>
    <sonar.coverage.exclusions>
        src/test/java/**,
        **/*Test.java
    </sonar.coverage.exclusions>
</properties>
```

## Technical Implementation

### Execution Data Merging
The plugin uses intelligent deduplication when processing multiple exec files:
- Tracks unique class IDs to prevent double-counting
- Merges execution data at the line level using JaCoCo's built-in merging
- Handles overlapping coverage from shared dependencies

### Pattern Matching
Supports both JaCoCo-style and Sonar-style exclusion patterns:
- **JaCoCo patterns**: `com/example/**/*.class`, `**/*Controller.class`
- **Sonar patterns**: `src/main/java/**`, `**/*Test.java`

### Console Rendering
Automatically detects terminal capabilities:
- Uses Unicode box-drawing characters (├─└─│) for UTF-8 terminals
- Falls back to ASCII characters (+-|\) for legacy terminals
- Handles Windows console code page detection via JNA

## Contributing

The plugin is designed to work with minimal configuration while providing extensive customization options for advanced use cases. Contributions are welcome!

### Building from Source
```bash
mvn clean install
```

### Running Tests
```bash
mvn test
```