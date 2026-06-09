<picture>
  <source media="(prefers-color-scheme: dark)" srcset="website/img/vulnlog-overview-dark.svg">
  <img alt="How Vulnlog works: scanner reports a finding, engineer analyses the impact, Vulnlog generates report and ignore files." src="website/img/vulnlog-overview.svg">
</picture>

# Vulnlog

**Supply chain security, as code. Track vulnerability findings in your repository.**

[![GitHub release](https://img.shields.io/github/v/release/vulnlog/vulnlog?color=%23f405c5)](https://github.com/vulnlog/vulnlog/releases)
[![Continuous Integration](https://github.com/vulnlog/vulnlog/actions/workflows/ci.yaml/badge.svg)](https://github.com/vulnlog/vulnlog/actions/workflows/ci.yaml)
[![License: Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

Vulnlog helps software maintainers to triage software vulnerabilities and communicate their impact on the software to
peers, consumers, or other systems.

Vulnlog runs in your CI pipeline and produces vulnerability reports and suppression/ignore files for Software
Composition Analysis (SCA) tools.

Vulnlog consists of a CLI application and a YAML-based vulnerability definition file in your Git repository. Vulnlog is open source and licensed under the Apache-2.0 license.

## Who is Vulnlog for?

| Audience                        | What Vulnlog gives them                                                                                                                 |
|---------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| **Software maintainers**        | Systematic, consistent triage of reported vulnerabilities, with the analysis and impact verdict recorded next to the code.              |
| **Product managers and owners** | The HTML Vulnerability Report with every finding and verdict in one place, to plan fix releases and track the product's security state. |
| **Customers and consumers**     | The published report, showing which vulnerabilities affect the software, their impact, and the version that fixes them.                 |
| **SCA scanners** (Snyk, Trivy)  | Up-to-date suppression/ignore files so CI scans stay green and known or irrelevant findings are not re-reported.                        |

## Why use Vulnlog?

Take **Log4Shell** ([CVE-2021-44228](https://en.wikipedia.org/wiki/Log4Shell)) as an example. Acme sells a JVM-based
Wiki product that depends on the Log4J library and runs two SCA scanners (Trivy and Snyk) nightly in CI. When
researchers disclose Log4Shell and Acme's next scan flags it as critical, Vulnlog turns the process into a workflow:

1. An engineer reviews the impact on the Wiki, records the analysis and verdict, and adds a temporary three-day
   suppression to the **Vulnlog YAML** file in the Git repository.
2. The next CI run invokes the **Vulnlog CLI**, regenerating the Trivy and Snyk **suppression/ignore files** and a fresh
   HTML **Vulnerability Report**.
3. The product manager reads the report and plans a release that contains the patched Log4J dependency.
4. Acme's customers raise a support ticket; the support team answers it straight from the published report, without
   pulling in the engineers.

## Quick Start

> [!NOTE]
> Vulnlog is in active development. The YAML format, CLI commands, and Gradle plugin may still change.
> Feedback and contributions are very welcome!

### Install the CLI

Pull the Docker image:

```sh
docker pull ghcr.io/vulnlog/vulnlog:latest
```

Or add the Gradle plugin to your build:

```kotlin
plugins {
    id("dev.vulnlog") version "<version>"
}
```

Native binaries and JVM distributions are available on the
[releases page](https://github.com/vulnlog/vulnlog/releases).

### Scaffold a new Vulnlog file

```sh
vulnlog init --organization "Acme Corp" --name "Acme Web App" --author "Security Team" -o vulnlog.yaml
```

This creates a minimal `vulnlog.yaml` you can start editing.

### Write your first entry

A Vulnlog file is plain YAML. Here is a minimal example:

```yaml
# $schema: https://vulnlog.dev/schema/vulnlog-v1.json
---
schemaVersion: "1"

project:
  organization: Acme Corp
  name: Acme Web App
  author: Security Team

releases:
  - id: 1.0.0
    published_at: 2026-01-15

vulnerabilities:

  - id: CVE-2026-1234
    releases: [ 1.0.0 ]
    description: Remote code execution in example-lib
    packages: [ "pkg:npm/example-lib@2.3.0" ]
    reports:
      - reporter: trivy
    analysis: >
      The vulnerable code path is not reachable in our application
      because we only use the safe subset of the API.
    verdict: not affected
    justification: vulnerable code not in execute path
```

### Validate and generate suppression files and an HTML report

```sh
# Check the file for errors
vulnlog validate vulnlog.yaml

# Generate a Trivy suppression file for release 1.0.0
vulnlog suppress vulnlog.yaml --reporter trivy --release 1.0.0

# Generate an HTML report
vulnlog report vulnlog.yaml
```

## Documentation

- [Documentation](https://vulnlog.dev/docs/)
- [Project Website](https://vulnlog.dev/)
- [Changelog](CHANGELOG.md)

## Community

[![Bluesky](https://img.shields.io/bluesky/followers/vulnlog.bsky.social?style=flat&logo=bluesky&labelColor=white&color=blue)](https://bsky.app/profile/vulnlog.bsky.social)
[![Mastodon](https://img.shields.io/mastodon/follow/114149693629631038?domain=infosec.exchange&style=flat&logo=mastodon&labelColor=white&color=blue)](https://infosec.exchange/@vulnlog)

## Contributing

Contributions are welcome! Whether it is a bug report, a docs fix, or a new feature -- check out
[CONTRIBUTING.md](CONTRIBUTING.md) to get started. If you are looking for something to pick up, look for issues
labelled **good first issue**.

⭐ If you find Vulnlog useful, giving it a star on GitHub helps others discover the project.

The Vulnlog contributors:

<a href="https://github.com/vulnlog/vulnlog/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=vulnlog/vulnlog" alt="Contributors" />
</a>
