---
title: Gradle Plugin
description: 
---

# {{ title }}

Apply the [Gradle Vulnlog Plugin](https://plugins.gradle.org/plugin/dev.vulnlog.dslplugin) to your
project to configure the Vulnlog CLI behaviour:

Example:

```kotlin
plugins {
    id("java")
    id("dev.vulnlog.dslplugin") version "$version"
}

vulnlog {
    version.set("0.7.1")
    definitionsFile.set(layout.projectDirectory.file("definitions.vl.kts"))
    reportOutput.set(layout.buildDirectory.dir("vulnlog-reports"))
    releaseBranch.addAll("Release Branch 0", "Release Branch 1")
}
```

| Field                                  | Required | Default                                                        | Description                                                                                                                          |
|----------------------------------------|----------|----------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| `definitionsFile: RegularFileProperty` | yes      | -                                                              | The location of the Vulnlog definitions file.                                                                                        |
| `reportOutput: DirectoryProperty`      | yes      | -                                                              | Specify the Vulnlog report output directory location.                                                                                |
| `releaseBranch: ListProperty<String>`  | no       | All release branches defined in the `definitions.vl.kts` file. | Specify one or multiple release branches to generate a report for. If not specified, reports for all release branches are generated. |
| `version: Property<String>`            | no       | Version of the Vulnlog Gradle Plugin                           | The version for DSL and CLI to use.                                                                                                  |
