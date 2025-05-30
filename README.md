<div style="text-align: center;"><img src="https://vulnlog.dev/logo/banner-1500x500-light-grey.png" alt="The Vulnlog draft project logo"/></div>

# Vulnlog – Software Vulnerability Tracking for Development Teams

![GitHub release](https://img.shields.io/github/v/release/vulnlog/vulnlog)
![Build Status](https://img.shields.io/github/actions/workflow/status/vulnlog/vulnlog/build.yml)
![License](https://img.shields.io/github/license/vulnlog/vulnlog)

Log and treat software vulnerabilities in one place.

Vulnlog is an open-source tool and DSL for documenting software vulnerabilities and integrating vulnerability reporting
and management into your development workflow.

Define reported vulnerabilities for your software project in a simple DSL. Let your CI pipeline generate reports for you
and your team.

![From the DSL to Reports](assets/dsl-to-reports.png)

**Caution: The project is still in early development, the DSL, CLI commands, the Gradle plugin and the HTML report are
subject to change.** Any feedback on the tool is very appreciated!

For more information, check out the [project website](https://vulnlog.dev/), the release change logs
in [CHANGELOG.md](CHANGELOG.md), the DSL troubleshooting guide in [TROUBLESHOOTING.md](TROUBLESHOOTING.md) and
the [DSL API documentation](https://vulnlog.dev/dslapi/latest/).

To see the Vulnlog in action, check out this [demo project](https://github.com/vulnlog/demo).

- [Quick Start](#quick-start)
- [DSL Reference](#dsl-reference)
- [CLI Reference](#cli-reference)
- [Gradle Plugin Reference](#gradle-plugin-reference)
- [HTML Report](#html-report)
- [Contributing & Support](#contributing--support)
- [License](#license)

## Quick Start

The easiest way is to use the [Gradle Vulnlog plugin](https://plugins.gradle.org/plugin/dev.vulnlog.dslplugin). Add the
Vulnlog DSL plugin to your `build.gradle.kts` file:

```kotlin
plugins {
    id("java")
    id("dev.vulnlog.dslplugin") version "$version"
}
```

Check that the Gradle plugin is correctly applied by running the `showCliVersion` task:

```
./gradlew showCliVersion
Vulnlog $version
```

### Generate your first Report

Create a Vulnlog definitions file that contains the release definitions and a vulnerability reporter for your project.
An example file is`definitions.vl.kts`:

```kotlin
releases {
    branch("branch 1") {
        release("0.1.0", "2025-01-01")
        release("0.1.1", "2025-01-23")
        release("0.2.0")
    }
    branch("branch 2") {
        release("2.0.0", "2025-02-01")
        release("2.1.0")
    }
}

reporters {
    reporter("demo reporter")
}
```

This defines two release branches `branch1` and `branch2` containing multiple releases. A release without a publication
date is still in development. Also, a reporter `demoReporter` is defined.

The next step is to create a project Vulnlog file containing your vulnerability analysis. For this demo example, the
file used is `demo.vl.kts`:

```kotlin
val branch1 by ReleaseBranchProvider
val branch2 by ReleaseBranchProvider

val demoReporter by ReporterProvider

vuln("CVE-1337-42") {
    report from demoReporter at "2025-01-28" on branch1..branch2
    analysis analysedAt "2025-01-30" verdict notAffected because """
        This is just a demo entry for demonstration purpose.
    """.trimIndent()
    task update "vulnerable.dependency" atLeastTo "1.2.3" on all
    execution suppress untilNextPublication on all
}
```

The first two lines introduce the two release branches. The third line introduces a reporter. The _CVE-1337-42_ is made
up for demonstration purposes.

- `report` describes which reporter found this CVE, when you became aware of it, and on which release branches the CVE
  was found.
- `analysis` describes when you analyzed this report and the verdict you assigned, with a reason.
- `task` describes what actions are needed to get rid of this report, usually a dependency update.
- `execution` describes what should be done with this report until it is fixed.

Now generate one report per release branch: `vl definitions.vl.kts report --output ./`. This should produce
`./report-branch1.html` and `./report-branch2.html`.

## DSL Reference

### Container Blocks

Top level DSL definitions.

| Function    | Parameters                                                                              | Return | Description                                                                  |
|-------------|-----------------------------------------------------------------------------------------|--------|------------------------------------------------------------------------------|
| `releases`  | [Release Block](#release-block)                                                         | -      | Top level defining a release block.                                          |
| `vuln`      | The ID or IDs of one or multiple vulnerability identifier and [Vuln Block](#vuln-block) | -      | Define a vulnerability entry with a single vulnerability ID or multiple IDs. |
| `reporters` | [Reporter Block](#reporter-block)                                                       | -      | Top level defining a reporters block.                                        |

### Providers

Providers to provide values from the `definitions.vl.kts` file.

| Provider        | Description                                                                                                                              |
|-----------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `ReleaseBranch` | Provide a release branch name. For example `val v1 by ReleaseBranch` provides the `v1` release branch  of the `definitions.vl.kts` file. |

### Specifiers

Release specifier.

| Specifier  | Description                                                       |
|------------|-------------------------------------------------------------------|
| `all`      | All releases that are defined in the report.                      |
| `allOther` | All other releases that are not already specified in a statement. |

Suppression specifier.

| Specifier              | Description                                                                |
|------------------------|----------------------------------------------------------------------------|
| `permanent`            | Permanently suppress a vulnerability.                                      |
| `temporarily`          | Temporarily suppress a vulnerability.                                      |
| `untilNextPublication` | Suppress a vulnerability until the successor release version is published. |

Verdict specifier.

| Specifier     | Description                                                                                  |
|---------------|----------------------------------------------------------------------------------------------|
| `critical`    | Vulnerability analysis resulted in critical impact to software project.                      |
| `high`        | Vulnerability analysis revealed high impact on software project.                             |
| `moderate`    | Vulnerability analysis revealed moderate impact on software project.                         |
| `low`         | Vulnerability analysis revealed low impact on software project.                              |
| `notAffected` | Vulnerability analysis revealed that the vulnerability does not affect the software project. |

### Release Block

Define a release version and optionally a publication date or a release branch.

| Function  | Parameters                                                   | Return                                        |
|-----------|--------------------------------------------------------------|-----------------------------------------------|
| `release` | The version string and optionally a publication date string. | -                                             |
| `branch`  | The release branch name string                               | [Release Branch Block](#Release-Branch-Block) |

#### Release Branch Block

Define a release version and optionally a publication date in YYYY-MM-dd.

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

Defines which reporter found the vulnerability.

| Function | Parameters                                  | Return                      |
|----------|---------------------------------------------|-----------------------------|
| `from`   | The reporter that found the vulnerability.  | [Report From](#Report-From) |
| `from`   | The reporters that found the vulnerability. | [Report From](#Report-From) |

##### Report From

Defines the date since when the software security engineering team is aware of this vulnerability.

| Function | Parameters                                                | Return                            |
|----------|-----------------------------------------------------------|-----------------------------------|
| `at`     | A date string in the format YYYY-MM-dd, e.g. `2025-03-07` | [Report From On](#Report-From-On) |

###### Report From On

Define on what release branches the reported vulnerability was found.

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

| Function       | Parameters                            | Return                                       | Description                                     |
|----------------|---------------------------------------|----------------------------------------------|-------------------------------------------------|
| `update`       | The dependency to update as string.   | [Task At Least To](#task-update-at-least-to) | Update a specific dependency.                   |
| `noActionOn`   | A Release specifier.                  | Starting point for [Execution](#execution)   | No action is needed.                            |
| `waitOnAllFor` | Duration to wait fore, e.g. `14.days` | Starting point for [Execution](#execution)   | Wait for the specified time and then reanalyse. |

##### Task Update At Least To

| Function    | Parameters        | Return                                           | Description                               |
|-------------|-------------------|--------------------------------------------------|-------------------------------------------|
| `atLeastTo` | A version string. | [Task On](#task-specify-release-branch-versions) | Update at least to the specified version. |

##### Task Specify Release Branch Versions

| Function | Parameters                                | Return                                                                                                      | 
|----------|-------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `on`     | A Release specifier.                      | Starting point for [Execution](#Execution) or [Task Follow UP Specification](#task-follow-up-specification) | 
| `on`     | A range of release branches e.g. `v1..v2` | Starting point for [Execution](#Execution) or [Task Follow UP Specification](#task-follow-up-specification) | 
| `on`     | A release branches e.g. `v1`              | Starting point for [Execution](#Execution) or [Task Follow UP Specification](#task-follow-up-specification) | 

##### Task Follow UP Specification

| Function             | Parameters                                | Return                                           | Description                                                   | 
|----------------------|-------------------------------------------|--------------------------------------------------|---------------------------------------------------------------|
| `andNoActionOn`      | A range of release branches e.g. `v1..v2` | Starting point for [Execution](#Execution)       | No further action required on specified release branch range. |
| `andNoActionOn`      | A release branches e.g. `v1`              | Starting point for [Execution](#Execution)       | No further action required on specified release branch.       |
| `andUpdateAtLeastTo` | A version string.                         | [Task On](#task-specify-release-branch-versions) | Update at least to the specified version.                     |

#### Execution

| Function   | Parameters                                                 | Return                                                            | Description                                                   |
|------------|------------------------------------------------------------|-------------------------------------------------------------------|---------------------------------------------------------------|
| `fixedAt`  | A date string in the format YYYY-MM-dd, e.g. `2025-03-07`. | [Task At Least To](#Execution-Specify-Release-Branch-Versions)    | Mark a vulnerability as fixed.                                |
| `suppress` | [`permanent`](#specifiers)                                 | [Task At Least To](#Execution-Specify-Release-Branch-Versions)    | Suppress a vulnerability permanently.                         |
| `suppress` | [`temporarily`](#specifiers)                               | [Execution Suppress Temporarily](#Execution-Suppress-Temporarily) | Suppress a vulnerability for a certain amount of time.        |
| `suppress` | [`untilNextPublication`](#specifiers)                      | [Task At Least To](#Execution-Specify-Release-Branch-Versions)    | Suppress a vulnerability until the next release is published. |

##### Execution Suppress Temporarily

| Function  | Parameters                            | Return                                                         | 
|-----------|---------------------------------------|----------------------------------------------------------------|
| `forTime` | Duration to wait fore, e.g. `14.days` | [Task At Least To](#Execution-Specify-Release-Branch-Versions) | 

##### Execution Specify Release Branch Versions

| Function | Parameters                                | Return                              | 
|----------|-------------------------------------------|-------------------------------------|
| `on`     | A Release specifier.                      | A complete [Execution](#Execution)  | 
| `on`     | A range of release branches e.g. `v1..v2` | A complete [Execution](#Execution)  | 
| `on`     | A release branches e.g. `v1`              | A complete  [Execution](#Execution) | 

### Reporter Block

Define a reporter to use in `vuln` definitions. A reporter is anything or anyone who makes you aware of a vulnerability
in your software project.

| Function   | Parameters                  | Return |
|------------|-----------------------------|--------|
| `reporter` | Define a specific reporter. | -      |

## CLI Reference

The Vulnlog CLI can interpret the Vulnlog DSL and generate reports. The latest version can be downloaded in
the [Release](https://github.com/vulnlog/vulnlog/releases) section.

```terminal
$ ./bin/vl --help
Usage: main [<options>] <vulnlogfile> <command> [<args>]...

  CLI application to parse Vulnlog files.

Options:
  --vuln=<text>...    Filter to specific vulnerabilities
  --branch=<text>...  Filter to specific branches
  -v, --version       Show version number and exit.
  -h, --help          Show this message and exit

Arguments:
  <vulnlogfile>  The Vulnlog definition files to read.

Commands:
  report  Generate a Vulnlog report files.
```

## HTML Report

The Vulnlog CLI generates reports from the Vulnlog DSL file:

![Example report generated from the Vulnlog DSL by the Vulnlog CLI](assets/example-report.png)

The generated HTML report contains a table with this top-level information:

- **ID**: Describes the vulnerability with an ID. The ID does not need a special format and usually comes from your SCA
  scanner. It is also possible to use an (internal) issue number of your ticketing system.
- **Status**: The status a reported vulnerability has. See [Status Description](#status-description) below for more
  information.
- **Rating**: The impact the vulnerability may have on the software.
- **Reasoning**: Reasoning behind the rating.
- **Task**: What is necessary to get rid of this vulnerability report.
- **Affected**: What versions are affected by the vulnerability.
- **Fix**: In what version the vulnerability is planned to be fixed.

### Status Description

- **Affected**: This vulnerability affects the software project.
- **Fixed**: This vulnerability does not anymore affect the software project.
- **Not Affected**: This vulnerability does not affect the software project.
- **Under Investigation**: If the software project is affected by this vulnerability is currently under investigation.
- **Unknown**: The state of this vulnerability report is not known.

The status of a vulnerability is calculated as follows:

```mermaid
flowchart LR
    A(Start) --> B
    B{is analysis\ndefined} -->|no| C(under investigation)
    B -->|yes| D{is verdict\nequals\nnot affecte}
    D -->|yes| E(not affected)
    D -->|no| F{is verdict\nnot not affected\nand execution has fixedAt\nor upcoming release\nis in the past}
    F -->|yes| G(fixed)
    F -->|no| H{is verdict\nnot not affected\nand upcoming release is not defined\nor upcoming release\nis in the future}
    H -->|yes| I(affected)
    H -->|no| J(unkown)
```

## Gradle Plugin Reference

```kotlin
vulnlog {

    /**
     * Specify the version of the Vulnlog DSL and CLI to use.
     * If not defined, the Vulnlog Gradle plugins version is used.
     */
    version: Property<String>

    /**
     * Specify the Vulnlog definitions file.
     */
    definitionsFile: RegularFileProperty

    /**
     * Specify one or multiple release branches to generate a report for.
     * If not specified, reports for all release branches are generated.
     */
    releaseBranch: ListProperty<String>

    /**
     * Specify the Vulnlog report output directory location.
     */
    reportOutput: DirectoryProperty
}
```

## Contributing & Support

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details on how to get started.

If you like the project, please consider giving it a star ⭐ and follow us on social media:

- Bluesky: [@vulnlog.bsky.social](https://bsky.app/profile/vulnlog.bsky.social)
- Mastodon: [@vulnlog@infosec.exchange](https://infosec.exchange/@vulnlog)

## License

Vulnlog is licensed under the [GPL-3.0 License](LICENSE).
