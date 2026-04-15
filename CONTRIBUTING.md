# Contributing to Vulnlog

Thank you for contributing! Every contribution helps, whether it is a bug report, a docs fix, or a new feature.

## Getting Started

**Prerequisites:** Git and JDK 17+ (Gradle will automatically provision JDK 25 for compilation via
[Foojay](https://github.com/gradle/foojay-toolchains)).

After cloning, install the pre-commit hook for automatic code formatting:

```terminal
./gradlew installGitHooks
```

Not sure where to begin? Look for issues labelled **good first issue**, try Vulnlog on your own project, or improve
the documentation. For questions or ideas, open a [GitHub Discussion](https://github.com/vulnlog/vulnlog/discussions).

## Making Changes

1. For significant changes, discuss them first via a
   [GitHub Issue](https://github.com/vulnlog/vulnlog/issues/new) or
   [Discussion](https://github.com/vulnlog/vulnlog/discussions).
2. Format code with `./gradlew ktlintFormat` if not using the pre-commit hook.
3. Verify that `./gradlew check` passes before opening a pull request.

## Commit Conventions

This project uses [Conventional Commits](https://www.conventionalcommits.org). Every commit message must follow
this format:

```
<type>: <description>
```

Use one of these types:

| Type       | Purpose                                               |
|------------|-------------------------------------------------------|
| `feat`     | New feature or capability                             |
| `fix`      | Bug fix                                               |
| `docs`     | Documentation changes                                 |
| `refactor` | Code restructuring without behavior change            |
| `perf`     | Performance improvement                               |
| `test`     | Adding or updating tests                              |
| `ci`       | CI/CD configuration and dependency updates            |
| `chore`    | Maintenance tasks (dependency updates, configs, etc.) |

Keep the subject line concise (50 characters or less), use imperative mood ("Add support for..." not "Added
support for..."), and do not end it with a period. Use the commit body to explain *what* and *why*, not *how*.

## Developer Certificate of Origin

This project uses the [Developer Certificate of Origin](https://developercertificate.org/) (DCO) to certify that
contributors have the right to submit their work under the project's license. Every commit must include a
`Signed-off-by`
trailer with the contributor's real name and email address.

Git provides the `-s` / `--signoff` flag for this:

```terminal
git commit -s -m "fix: Correct YAML parsing for empty lists"
```

If you forgot to sign off, amend the most recent commit:

```terminal
git commit --amend -s --no-edit
```

Commits without a valid `Signed-off-by` trailer will fail the CI check. See the full
[DCO text](https://developercertificate.org/) for details.

## Reporting Bugs

File bugs via [GitHub Issues](https://github.com/vulnlog/vulnlog/issues/new) and include:

- What you were trying to achieve and the impact.
- The output of `vulnlog --version`.
- The complete error message and steps to reproduce.

## Licensing

This project is licensed under the [Apache License, Version 2.0](LICENSE). By contributing, you agree that your
contributions will be licensed under the same terms.
