# Vulnlog

Software vulnerability logging for developers.

If you are responsible for analysing and handling software vulnerabilities reported by _Software Component Analyser (
SCA)_. Then Vulnlog might be for you. Vulnlog provides a simple _Domain Specific Language (DSL)_ to describe reported
vulnerabilities. The DSL further allows you to define how and when the reported vulnerability should be handled. For
example in the next bug fix release a vulnerable dependency should be updated to version x.

Vulnlog helps you in your software project:

- Log all reported software vulnerabilities in your repository.
- Create vulnerability reports for your peers and project owner or manager.
- Don't forget to update vulnerable dependencies in your next bug fix release.

## How to use Vulnlog in your Project

The simples way is to use the Gradle Vulnlog plugin. Add the Vulnlog DSL plugin in your `build.gradle.kts` file:

```kotlin
plugins {
    id("java")
    id("dev.vulnlog.dslplugin") version "0.5.0-SNAPSHOT"
}
```

Verify that the Gradle plugin is applied correctly by running the task `showCliVersion`:

```
./gradlew showCliVersion
Vulnlog 0.5.0-SNAPSHOT
```

Create a Vulnlog definitions file containing release definitions of your project. An example `definitions.vl.kts`file:

```kotlin
releases {
    branch("v1") {
        release("0.1.0", "2025-01-01")
        release("0.1.1", "2025-01-23")
        release("0.2.0")
    }
    branch("v2") {
        release("2.0.0", "2025-02-01")
        release("2.1.0")
    }
}
```

This defines two release branches `v1` and `v2` containing multiple releases (`0.1.0`, `0.1.1` `0.2.0`, `2.0.0` and
`2.1.0`). A release without a publication date is still in development.

Next, a project Vulnlog file with is created containing your vulnerability analysis. For this demo example the file
`demo.vl.kts` is used:

```kotlin
val v1 by ReleaseBranch
val v2 by ReleaseBranch

vuln("CVE-1337-42") {
    report from SCA_SCANNER at "2025-01-28" on v1..v2
    analysis analysedAt "2025-01-30" verdict "not affected" because """
        This is just a demo entry for demonstration purpose.
    """.trimIndent()
    task update "vulnerable.dependency" atLeastTo "1.2.3" on all
    execution suppress untilNextPublication on all
}
```

The two release branches `v1` and `v2` are introduced on the first two lines. The CVE `CVE-1337-42` is made up for
demonstration purposes.

- `report` describes what reporter found this CVE, when you get aware of it and on what release branches the CVE was
  found.
- `analysis` describes when you analysed this report and the verdict you assign with a reasoning.
- `task` describes what actions are needed to get rid of this report. Usually a dependency must be updated.
- `execution` describes what should be done with this report until it is fixed.

Now generate a report per release branch: `vl definitions.vl.kts report --output ./`. This should generate
`./report-v1.html` and `./report-v2.html`.

## DSL

Top level DLS definitions.

| Function   | Parameters                                                                              | Return | Description                                                                  |
|------------|-----------------------------------------------------------------------------------------|--------|------------------------------------------------------------------------------|
| `releases` | [Release Block](#Release-Block)                                                         | -      | Top level defining a release block.                                          |
| `vuln`     | The ID or IDs of one or multiple vulnerability identifier and [Vuln Block](#Vuln-Block) | -      | Define a vulnerability entry with a single vulnerability ID or multiple IDs. |

Providers to provide values from the `definitions.vl.kts` file.

| Provider        | Description                                                                                                                              |
|-----------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `ReleaseBranch` | Provide a release branch name. For example `val v1 by ReleaseBranch` provides the `v1` release branch  of the `definitions.vl.kts` file. |

Release specifier.

| Specifier  | Description                                                       |
|------------|-------------------------------------------------------------------|
| `all`      | All releases that are defined in the report.                      |
| `allOther` | All other releases that are not already specified in a statement. |

### Release Block

Define a release version and optionally a publication date or a release branch.
Release version without publication date are still in development.

| Function  | Parameters                                                   | Return                                        |
|-----------|--------------------------------------------------------------|-----------------------------------------------|
| `release` | The version string and optionally a publication date string. | -                                             |
| `branch`  | The release branch name string                               | [Release Branch Block](#Release-Branch-Block) |

#### Release Branch Block

Define a release version and optionally a publication date in YYYY-MM-dd. Release version without publication date are
in development.

| Function  | Parameters                                                   | Return |
|-----------|--------------------------------------------------------------|--------|
| `release` | The version string and optionally a publication date string. | -      |

### Vuln Block

| Field                     | Description                                                                        |
|---------------------------|------------------------------------------------------------------------------------|
| [`report`](#Report)       | Contains the report information.                                                   |
| [`analysis`](#analysis)   | Contains the analysis information, requires a definition of the `report` variable. |
| [`task`](#Task)           | Contains the task information, requires a definition of the `analysis` variable.   |
| [`execution`](#Execution) | Contains the execution information, requires a definition of the `task` variable.  |

#### Report

Defines what reporter found the vulnerability.

| Function | Parameters                                 | Return                      |
|----------|--------------------------------------------|-----------------------------|
| `from`   | The reporter that found the vulnerability. | [Report From](#Report-From) |

##### Report From

Defines the date since when the software security engineering team is aware of this vulnerability.

| Function | Parameters                                                | Return                            |
|----------|-----------------------------------------------------------|-----------------------------------|
| `at`     | A date string in the format YYYY-MM-dd, e.g. `2025-03-07` | [Report From On](#Report-From-On) |

###### Report From On

Define on what release branches the reported vulnerability were found.

| Function | Parameters                                | Return                                   |
|----------|-------------------------------------------|------------------------------------------|
| `on`     | A range of release branches e.g. `v1..v2` | Starting point for [Analysis](#Analysis) |

#### Analysis

| Function     | Parameters                                                                                                  | Return                                                |
|--------------|-------------------------------------------------------------------------------------------------------------|-------------------------------------------------------|
| `analysedAt` | A date string in the format YYYY-MM-dd, e.g. `2025-03-07`. If not specified the date of the report is used. | [Analysis Analysed At Verdict](#Analysis-Analysed-At) |
| `verdict`    | A verdict based on the analysis of the report on the software project.                                      | [Analysis Reasoning](#Analysis-Reasoning)             |

##### Analysis Analysed At

| Function  | Parameters                                                             | Return                                    |
|-----------|------------------------------------------------------------------------|-------------------------------------------|
| `verdict` | A verdict based on the analysis of the report on the software project. | [Analysis Reasoning](#Analysis-Reasoning) |

##### Analysis Reasoning

| Function  | Parameters                                | Return                           |
|-----------|-------------------------------------------|----------------------------------|
| `because` | The reasoning why the verdict was chosen. | Starting point for [Task](#Task) |

#### Task

| Function       | Parameters                            | Return                                                    | Description                                     |
|----------------|---------------------------------------|-----------------------------------------------------------|-------------------------------------------------|
| `update`       | The dependency to update as string.   | [Task At Least To](#Task-Specify-Release-Branch-Versions) | Update a specific dependency.                   |
| `noActoinOn`   | A Release specifier.                  | Starting point for [Execution](#Execution)                | No action is needed.                            |
| `waitOnAllFor` | Duration to wait fore, e.g. `14.days` | Starting point for [Execution](#Execution)                | Wait for the specified time and then reanalyse. |

##### Task Specify Release Branch Versions

| Function | Parameters                                | Return                                     | 
|----------|-------------------------------------------|--------------------------------------------|
| `on`     | A Release specifier.                      | Starting point for [Execution](#Execution) | 
| `on`     | A range of release branches e.g. `v1..v2` | Starting point for [Execution](#Execution) | 
| `on`     | A release branches e.g. `v1`              | Starting point for [Execution](#Execution) | 

#### Execution

| Function   | Parameters             | Return                                                            | Description                                                   |
|------------|------------------------|-------------------------------------------------------------------|---------------------------------------------------------------|
| `suppress` | `permanent`            | [Task At Least To](#Execution-Specify-Release-Branch-Versions)    | Suppress a vulnerability permanently.                         |
| `suppress` | `temporarily`          | [Execution Suppress Temporarily](#Execution-Suppress-Temporarily) | Suppress a vulnerability for a certain amount of time.        |
| `suppress` | `untilNextPublication` | [Task At Least To](#Execution-Specify-Release-Branch-Versions)    | Suppress a vulnerability until the next release is published. |

##### Execution Suppress Temporarily

| Function  | Parameters                            | Return                                                         | 
|-----------|---------------------------------------|----------------------------------------------------------------|
| `forTime` | Duration to wait fore, e.g. `14.days` | [Task At Least To](#Execution-Specify-Release-Branch-Versions) | 

##### Execution Specify Release Branch Versions

| Function | Parameters                                | Return                                     | 
|----------|-------------------------------------------|--------------------------------------------|
| `on`     | A Release specifier.                      | Starting point for [Execution](#Execution) | 
| `on`     | A range of release branches e.g. `v1..v2` | Starting point for [Execution](#Execution) | 
| `on`     | A release branches e.g. `v1`              | Starting point for [Execution](#Execution) | 
