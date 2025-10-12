# Vulnlog Contributor’s Guide

Welcome to the Vulnlog community, and thank you for contributing! This guide explains how to get involved.

* [Licensing](#licensing)
* [Issue Tracking](#issue-tracking)
* [Pull Requests](#pull-requests)
* [Maintainers](#maintainers)

## Licensing

By contributing, you agree that your contributions will be licensed under the same license as this project
(see [LICENSE](LICENSE)).

## Issue Tracking

To file a bug or feature request, use [GitHub](https://github.com/vulnlog/vulnlog/issues/new). Be sure to include the
following information:

* Context
    * What are/were you trying to achieve?
    * What’s the impact of this bug/feature?

For bug reports, additionally include the following information:

* The output of vl --version.
* The complete error message.
* The simplest possible steps to reproduce.

## Pull Requests

When preparing a pull request, follow this checklist:

* Imitate the conventions of surrounding code.
* Format code with ./gradlew ktlintFormat (otherwise the build will fail).
* Verify that the JVM build (./gradlew build) succeeds.
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
starting [GitHub Discussion](https://github.com/vulnlog/vulnlog/discussions). This will save time and increase the
chance of your pull request being accepted.

## Maintainers

The project’s maintainers (those with write access to the upstream repository) are listed
in [MAINTAINERS.md](MAINTAINERS.md).
