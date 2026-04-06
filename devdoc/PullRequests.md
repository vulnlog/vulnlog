# Pull Requests

## Merge strategy

All PRs are **squash-merged**. Each PR becomes a single commit on `main`.

The PR title is used as the squash commit message and must follow the
[Conventional Commits](https://www.conventionalcommits.org) format:

```
feat: Add stdin support for validate command
fix: Correct release filter when no releases defined
docs: Update CLI examples in quickstart
```

This keeps the history linear and ensures the auto-generated changelog only contains meaningful
entries.

## GitHub repository settings

Under Settings > General > Pull Requests:

- Only **"Allow squash merging"** is enabled (merge commits and rebase merging are disabled).
- Default commit message is set to **"Pull request title"**.

## PR title validation

PR titles are checked by CI to enforce the conventional commit format. A PR with a title like
"fixed stuff" will fail the check — use `fix: Correct ...` instead.

## Changelog impact

The PR title directly determines how the change appears in the [CHANGELOG](../CHANGELOG.md):

| Prefix      | Changelog section | Example                              |
|-------------|-------------------|--------------------------------------|
| `feat:`     | Added             | `feat: Add Trivy native suppression` |
| `fix:`      | Fixed             | `fix: Handle empty YAML files`       |
| `docs:`     | Documentation     | `docs: Update quickstart guide`      |
| `refactor:` | Changed           | `refactor: Simplify parse module`    |
| `perf:`     | Performance       | `perf: Speed up YAML parsing`        |
| `chore:`    | *(skipped)*       | `chore: Bump Gradle to 9.5`          |
| `ci:`       | *(skipped)*       | `ci: Fix macOS build matrix`         |
| `test:`     | *(skipped)*       | `test: Add suppression edge cases`   |
