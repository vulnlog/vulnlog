[![Vulnlog](https://vulnlog.dev/logo/banner-1500x500-light-grey.png)](https://github.com/vulnlog/vulnlog)

:star: Please star us on [Github](https://github.com/vulnlog/vulnlog) to promote the project!

[![GitHub release](https://img.shields.io/github/v/release/vulnlog/vulnlog?color=%23f405c5)](https://github.com/vulnlog/vulnlog/releases)
[![Continuous Integration](https://github.com/vulnlog/vulnlog/actions/workflows/ci.yaml/badge.svg)](https://github.com/vulnlog/vulnlog/actions/workflows/ci.yaml)

Vulnlog is a tool that enables you to track, organise and communicate reported software vulnerabilities all in one
place.

It consists of a domain-specific language (DSL) for documenting software vulnerability analysis within your source code
repository, as well as a command-line interface (CLI) application for generating HTML reports and suppression files (in
the upcoming release). Vulnlog is designed to be used in your CI pipeline to automate the generation of HTML reports and
suppression files.

**Caution: The project is still in early development, the DSL, CLI commands, the Gradle plugin and the HTML report are
subject to change.** Any feedback on the tool is very appreciated!

## Features

- A simple DSL for documenting reported software vulnerabilities in one place.
- Supports multiple parallel release branches.
- Automated generation of HTML vulnerability reports to communicate your analysis and promote transparency.
- Automated generation of suppression files for software component analysis (SCA) scanners in the upcoming release.
- A Gradle plugin to easily integrate Vulnlog into existing workflows.
- A CLI tool for use locally or within your CI pipeline.

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

## Generate your first Report

Create a Vulnlog definitions file containing the release definitions and a vulnerability reporter for your project.
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

This defines two release branches, `branch1` and `branch2`, which contain multiple releases. A release without a
publication date is still in development. Also, a reporter, `demoReporter`, is defined.

The next step is to create a Vulnlog project file containing your vulnerability analysis. For this demo example, the
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

The first two lines introduce the two release branches. The third line introduces the reporter. _CVE-1337-42_ has been
created for demonstration purposes.

- `report` describes which reporter found this CVE, when you became aware of it and on which release branches the CVE
  was found.
- `analysis` describes when the report was analysed and the verdict assigned, along with the reasoning behind it.
- `task` describes the actions needed to resolve this issue, which is usually a dependency update.
- `execution` section describes what should be done with the report until it is fixed.

Generate one report per release branch: `vl definitions.vl.kts report --output ./` This should produce
`./report-branch1.html` and `./report-branch2.html`.

## Documentation and more Information

For more information, check out the [project website](https://vulnlog.dev/), the release change logs
in [CHANGELOG.md](CHANGELOG.md), the DSL troubleshooting guide in [TROUBLESHOOTING.md](TROUBLESHOOTING.md) and
the [DSL API documentation](https://vulnlog.dev/dslapi/latest/).

To see the Vulnlog in action, check out this [demo project](https://github.com/vulnlog/demo).

- [Getting Started](https://vulnlog.dev/getting-started/)
- [DSL Reference](https://vulnlog.dev/documentation/)

## Contributors

Thanks goes to these wonderful people ✨

<a href="https://github.com/vulnlog/vulnlog/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=vulnlog/vulnlog"  alt="List of all contributors."/>
</a>

Made with [contrib.rocks](https://contrib.rocks).

## Socials

[![Bluesky followers](https://img.shields.io/bluesky/followers/vulnlog.bsky.social?style=flat&logo=bluesky&labelColor=white&color=blue)](https://bsky.app/profile/vulnlog.bsky.social)
[![Mastodon followers](https://img.shields.io/mastodon/follow/114149693629631038?domain=infosec.exchange&style=flat&logo=mastodon&labelColor=white&color=blue)](https://infosec.exchange/@vulnlog)

## License

Vulnlog is licensed under the [GPL-3.0 License](LICENSE).
