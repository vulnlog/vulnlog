# Vulnlog

[![GitHub release](https://img.shields.io/github/v/release/vulnlog/vulnlog?color=%23f405c5)](https://github.com/vulnlog/vulnlog/releases)
[![Continuous Integration](https://github.com/vulnlog/vulnlog/actions/workflows/ci.yaml/badge.svg)](https://github.com/vulnlog/vulnlog/actions/workflows/ci.yaml)
[![License: Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

Vulnlog is a YAML format for recording how vulnerabilities affect your software. You record your analysis once, in a
Vulnlog file in your Git repository. The Vulnlog CLI and the Gradle plugin generate from it what your scanners, your
team, and your customers need:

- Suppression files, so your scanners stop reporting findings you've analysed. Trivy, Snyk, OWASP Dependency-Check, and
  cargo-audit get their own format. Every other scanner gets a generic JSON format.
- An HTML impact report that shows which vulnerabilities affect each release.
- OpenVEX documents, which tell your customers' tools whether a release is affected by a vulnerability.

Vulnlog doesn't scan your software. It works alongside your scanners.

> [!NOTE]
> Vulnlog is in active development. The YAML format, the CLI commands, and the Gradle plugin may still change.

## Get started

Install the CLI:

```sh
curl -fsSL vulnlog.dev/install | sh
```

For every other installation method and a first tutorial, see the [documentation](https://vulnlog.dev/docs/).

## Learn more

- [Website](https://vulnlog.dev/)
- [Documentation](https://vulnlog.dev/docs/)
- [Changelog](CHANGELOG.md)

## Community

- Ask a question or propose a feature in [GitHub Discussions](https://github.com/vulnlog/vulnlog/discussions).
- If you use Vulnlog, tell us about your use cases
  in [Adopters](https://github.com/vulnlog/vulnlog/discussions/categories/adopters).
- Follow Vulnlog on [Bluesky](https://bsky.app/profile/vulnlog.bsky.social)
  and [Mastodon](https://infosec.exchange/@vulnlog).
- To get help, see [SUPPORT.md](SUPPORT.md). To report a vulnerability in Vulnlog, see [SECURITY.md](SECURITY.md).

If Vulnlog is useful to you, star the repository. It helps others find the project.

## Contributing

Contributions are welcome, from a bug report to a new feature. See [CONTRIBUTING.md](CONTRIBUTING.md). If you're looking
for something to pick up, look for issues labelled `good first issue`.

Thank you to all [contributors](https://github.com/vulnlog/vulnlog/graphs/contributors).

## Licence

Vulnlog is free and open source under the [Apache License 2.0](LICENSE).
