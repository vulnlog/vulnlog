---
title: DSL Reference
description: 
---

# {{ title }}

The Vulnlog DSL is Kotlin-based and can be used in `*.vl.kts` files. The [Vulnlog CLI](../cli/) can
parse the DSL.

The Vulnlog CLI requires a `definitions.vl.kts` file and reads all `*.vl.kts` files in the same
folder as the definition file.

* `definitins.vl.kts` defines all resources and configurations that are available in a Vulnlog file.
  It is similar to a Gradle settings file.
* `*.vl.kts` contains the vulnerability reports.

Example:

```kotlin
// definitions.vl.kts
releases {
    branch("Release Branch 0") {
        release("0.1.0")
    }
}

reporters {
    reporter("OWASP Dependency Check")
}
```

```kotlin
// demo.vl.kts
val releaseBranch0 by ReleaseBranchProvider
val owaspDependencyCheck by ReporterProvider

vuln("CVE-2025-005") {
    report from dependencyScanner1 at "2025-04-08" on releaseBranch0..releaseBranch0
}
```

The Vulnlog DSL consists of context or block definitions, providers and specifiers.

## Context Definitions

Top level context definitions. The DSL consists of these functions with their context or blocks.

| Function                                                     | Use in file          | Description                                               |
|--------------------------------------------------------------|----------------------|-----------------------------------------------------------|
| [`releases`](/documentation/dsl/releases-context)            | `definitions.vl.kts` | Define releases and release branches within this context. |
| [`reporters`](/documentation/dsl/reporters-context)          | `definitions.vl.kts` | Define reporters within this context                      |
| [`vuln(vararg id: String)`](/documentation/dsl/vuln-context) | `*.vl.kts`           | Define a vulnerability entry within this context.         |

## Providers

Providers provide definitions from within the `definitions.vl.kts` file in a `*.vl.kts` file.

| Provider                | Description                                                                               |
|-------------------------|-------------------------------------------------------------------------------------------|
| `ReleaseBranchProvider` | Provide a release branch from the `definitions.vl.kts` file in a `*.vl.kts` Vulnlog file. |
| `ReporterProvider`      | Provide a reporter from the `definitions.vl.kts` file in a `*.vl.kts` Vulnlog file.       |

Example:

```kotlin
// The definition file has a release branch definition of "Release Branch 0" that
// is providable by its camel-case version.
val releaseBranch0 by ReleaseBranchProvider

// The definition file has a reporter definition of "Dependency Scanner 1" that
// is providable by its camel-case version.
val dependencyScanner1 by ReporterProvider
```

## Specifiers

Specifiers are DSL constants values allowing to configure or define functions within the DSL.

### Release Branch Specifiers

Release branch specifiers help to describe a set of release branches.

| Specifier  | Description                                                       |
|------------|-------------------------------------------------------------------|
| `all`      | All releases that are defined in the report.                      |
| `allOther` | All other releases that are not already specified in a statement. |

### Suppression Specifiers

Suppress specifiers help to describe how long a suppression is active.

| Specifier              | Description                                                                |
|------------------------|----------------------------------------------------------------------------|
| `permanent`            | Permanently suppress a vulnerability.                                      |
| `temporarily`          | Temporarily suppress a vulnerability requires a duration specification.    |
| `untilNextPublication` | Suppress a vulnerability until the successor release version is published. |

### Verdict Specifier

Verdict specifiers define what severity level a vulnerability regarding the software project has. A
`critical` verdict has a more citical impact on the software project than a `low` verdict. If the
project is not at all affected or has mitigations in place the `notAffected` verdict is used.

| Specifier     | Description                                                                                  |
|---------------|----------------------------------------------------------------------------------------------|
| `critical`    | Vulnerability analysis resulted in critical impact to software project.                      |
| `high`        | Vulnerability analysis revealed high impact on software project.                             |
| `moderate`    | Vulnerability analysis revealed moderate impact on software project.                         |
| `low`         | Vulnerability analysis revealed low impact on software project.                              |
| `notAffected` | Vulnerability analysis revealed that the vulnerability does not affect the software project. |
