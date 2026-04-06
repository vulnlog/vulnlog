# Vulnlog Contributor's Guide

Welcome to the Vulnlog community, and thank you for contributing! Whether you are fixing a typo, reporting a bug,
improving documentation, or adding a feature -- every contribution helps.

* [Prerequisites](#prerequisites)
* [Local Setup](#local-setup)
* [Where to Start](#where-to-start)
* [Issue Tracking](#issue-tracking)
* [Pull Requests](#pull-requests)
* [Licensing](#licensing)
* [Maintainers](#maintainers)

## Prerequisites

The following tools are required to build and contribute to this project:

- **JDK 17+** — the Gradle toolchain will automatically provision JDK 25 for compilation via
  [Foojay](https://github.com/gradle/foojay-toolchains), so any JDK 17+ suffices to bootstrap Gradle.
  The project targets JVM 17 bytecode.
- **Git**

Optional, only needed to build native images locally:

- **GraalVM CE 21** — install via [SDKMAN](https://sdkman.io) (`sdk install java 21.0.x-graal`) or
  [GraalVM's releases](https://github.com/graalvm/graalvm-ce-builds/releases)

## Local Setup

After cloning, install the ktlint pre-commit hook so your code is automatically formatted before each commit:

```terminal
./gradlew installGitHooks
```

This uses the project's own ktlint version via Gradle — no separate `ktlint` installation required.

## Where to Start

Not sure where to begin? Here are a few ideas:

- Look for issues labelled **good first issue** — these are scoped to be approachable for newcomers.
- Try using Vulnlog on a project of your own and report any rough edges you find.
- Improve or clarify the documentation — fresh eyes catch things maintainers miss.

If you have a question or want to bounce an idea around before writing code, feel free to open
a [GitHub Discussion](https://github.com/vulnlog/vulnlog/discussions).

## Issue Tracking

To file a bug or feature request, use [GitHub Issues](https://github.com/vulnlog/vulnlog/issues/new). Please include:

* Context
    * What are/were you trying to achieve?
    * What's the impact of this bug/feature?

For bug reports, additionally include:

* The output of `vulnlog --version`.
* The complete error message.
* The simplest possible steps to reproduce.

## Pull Requests

When preparing a pull request, follow this checklist:

* Imitate the conventions of surrounding code.
* Format code with `./gradlew ktlintFormat` if not using the pre-commit hook.
* Verify that `./gradlew check` passes.
* For native image builds: `./gradlew :next:nativeCompile --no-configuration-cache`.
* Use [conventional commit](https://www.conventionalcommits.org) messages.
* Follow the seven rules of great Git commit messages:
    * Separate the subject from the body with a blank line.
    * Limit the subject line to 50 characters.
    * Capitalize the subject line.
    * Do not end the subject line with a period.
    * Use the imperative mood in the subject line.
    * Wrap the body at 72 characters.
    * Use the body to explain what and why vs. how.

Important: If you plan to make significant changes or add new features, we encourage you to first discuss them with the
wider Vulnlog developer community. You can do this by filing
a [GitHub Issue](https://github.com/vulnlog/vulnlog/issues/new) or by
starting a [GitHub Discussion](https://github.com/vulnlog/vulnlog/discussions). This will save time and increase the
chance of your pull request being accepted.

## Licensing

By contributing, you agree that your contributions will be licensed under the same license as this project
(see [LICENSE](LICENSE)).

## Maintainers

The project's maintainers (those with write access to the upstream repository) are listed
in [MAINTAINERS.md](MAINTAINERS.md).
