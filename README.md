[![Vulnlog](assets/banner-1500x500-light-grey.png)](https://github.com/vulnlog/vulnlog)

[![GitHub release](https://img.shields.io/github/v/release/vulnlog/vulnlog?color=%23f405c5)](https://github.com/vulnlog/vulnlog/releases)
[![Continuous Integration](https://github.com/vulnlog/vulnlog/actions/workflows/ci.yaml/badge.svg)](https://github.com/vulnlog/vulnlog/actions/workflows/ci.yaml)

Vulnlog is an open-source tool for tracking, documenting and communicating vulnerability analysis directly in your
source code repository. It uses a simple YAML format for recording your findings and a CLI for generating suppression
files for common SCA scanners.

> **Note:** Vulnlog is in active development. The YAML format, CLI commands and Gradle plugin may still change.
> Feedback and contributions are very welcome!

## Why Vulnlog?

SCA scanners find vulnerabilities, but the analysis, triage and reasoning usually live in tickets, spreadsheets or
someone's head. Vulnlog gives that context a home right next to your code:

- **One place for your analysis** -- document verdicts, justifications and resolution plans in version-controlled YAML.
- **Suppression file generation** -- feed your analysis back into scanners like Trivy, Snyk, Dependency-Check, Grype
  and others so they stop flagging what you have already reviewed.
- **Works with your workflow** -- use the CLI locally, in CI, or via the Gradle plugin.

## Quick Start

### Install the CLI

Download a native binary from the [latest release](https://github.com/vulnlog/vulnlog/releases), or pull the
Docker image:

```sh
docker pull ghcr.io/vulnlog/vulnlog:latest
```

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

### Validate and generate suppression files

```sh
# Check the file for errors
vulnlog validate vulnlog.yaml

# Generate suppression files for all reporters
vulnlog suppress vulnlog.yaml -o ./suppressions/

# Or for a single reporter, written to stdout
vulnlog suppress vulnlog.yaml --reporter trivy -o -
```

## Documentation

- [Getting Started](https://vulnlog.dev/docs/vulnlog/0.11.0/quickstart.html)
- [Project Website](https://vulnlog.dev/)
- [Documentation](https://vulnlog.dev/docs/)
- [Changelog](CHANGELOG.md)

## Community

[![Bluesky](https://img.shields.io/bluesky/followers/vulnlog.bsky.social?style=flat&logo=bluesky&labelColor=white&color=blue)](https://bsky.app/profile/vulnlog.bsky.social)
[![Mastodon](https://img.shields.io/mastodon/follow/114149693629631038?domain=infosec.exchange&style=flat&logo=mastodon&labelColor=white&color=blue)](https://infosec.exchange/@vulnlog)

## Contributing

Contributions are welcome! Whether it is a bug report, a docs fix, or a new feature -- check out
[CONTRIBUTING.md](CONTRIBUTING.md) to get started. If you are looking for something to pick up, look for issues
labelled **good first issue**.

:star: If you find Vulnlog useful, giving it a star on GitHub helps others discover the project.

Thanks go to all contributors:

<a href="https://github.com/vulnlog/vulnlog/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=vulnlog/vulnlog" alt="Contributors" />
</a>
