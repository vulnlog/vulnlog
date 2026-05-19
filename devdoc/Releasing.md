# Releasing

## Developer builds

Running or building locally without passing a version produces a developer build automatically:

```terminal
./gradlew :cli-app:run
# → Vulnlog SNAPSHOT+a1b2c3d
```

The version is `SNAPSHOT+<git-short-hash>`, so it is always traceable to a specific commit.

## Release builds (next CLI)

Releases are driven by version tags. The pipeline triggers on tags matching
`v[0-9]+.[0-9]+.[0-9]+` (e.g. `v0.10.0`, `v0.11.2`, `v1.0.0`); maintenance tags `v0.9.*` are
excluded and handled by [`cd-0.9.yaml`](../.github/workflows/cd-0.9.yaml). The pipeline builds
and publishes artifacts only — it does not write `CHANGELOG.md` and does not generate the
GitHub-release body. Both are prepared by the maintainer.

### 1. Tag and push

Tag a commit on `main` with the version (with the `v` prefix on the tag, without it in the
build) and push:

```terminal
git tag v0.12.0
git push origin v0.12.0
```

The pipeline runs the following jobs, parallelised where dependencies allow:

- **`build-native-images`** — Linux, macOS, and Windows native images in parallel
- **`publish-gradle-plugin`** — publishes to the Gradle Plugin Portal (independent of the native build)
- **`publish-release`** (after `build-native-images`) — builds the JVM distribution zip, creates a GitHub release
  marked as pre-release with all four artifacts attached and a body containing GitHub's auto-generated "What's
  Changed" list plus a link to `CHANGELOG.md`, and posts a release announcement in GitHub Discussions
- **`publish-docker`** (after `build-native-images`) — pushes `ghcr.io` images with both `:<version>` and `:latest` tags
- **`deploy-pages`** (after `publish-release`) — deploys the website and Antora docs to GitHub Pages

### 2. Fill in the GitHub-release body

The release lands as pre-release with the auto-generated GitHub "What's Changed" list (from PR
titles) plus a link to `CHANGELOG.md`. Review it in the GitHub UI and refine as needed.

To start from a richer draft based on conventional-commit grouping (`Features`, `Bug Fixes`, …),
generate it locally with the release-notes config and paste it in:

```terminal
git-cliff --config .github/release-notes.toml \
  --latest --github-repo vulnlog/vulnlog \
  --github-token "$(gh auth token)"
```

The `--github-token` keeps the call authenticated and avoids GitHub's unauthenticated API rate
limit. Edit the body (fix PR-title typos, add highlights, link to docs) and save. Then un-flag
the pre-release toggle to publish.

### 3. Update `CHANGELOG.md`

On a branch off `main`, prepend the new release section with `git-cliff` and update the
footer compare-link list:

```terminal
git checkout -b chore/changelog-v0.12.0 main
git-cliff --config .github/changelog.toml \
  --unreleased --tag v0.12.0 \
  --github-repo vulnlog/vulnlog \
  --prepend CHANGELOG.md
```

`git-cliff --prepend` rewrites `CHANGELOG.md` in place: it strips the existing `# Changelog`
header, prepends the new `## [<version>]` section, and re-emits the header on top. It does
**not** regenerate the footer compare-link block — add the new entry yourself at the top of
that block, following the existing convention (label without `v` prefix, URL tags with `v`):

```
[<version>]: https://github.com/vulnlog/vulnlog/compare/<prev-tag>...v<version>
```

`git-cliff` renders the squashed PR-title commit subject verbatim, so any PR-title typo lands
in the new section. Fix it here (see [Pull Requests](Pull-Requests.md) for the title
conventions). Commit, open a PR against `main`, and merge it.

This step can be done before tagging or after the release lands — the pipeline is independent.

To preview the result before committing, prepend into a copy and diff:

```terminal
cp CHANGELOG.md /tmp/CHANGELOG.preview.md
git-cliff --config .github/changelog.toml \
  --unreleased --tag v0.12.0 \
  --github-repo vulnlog/vulnlog \
  --prepend /tmp/CHANGELOG.preview.md
diff -u CHANGELOG.md /tmp/CHANGELOG.preview.md
rm /tmp/CHANGELOG.preview.md
```

## Release candidates

To validate the final binaries before cutting a release, tag a release candidate first.
RC tags match `v[0-9]+.[0-9]+.[0-9]+-rc[0-9]+` (e.g. `v0.12.0-rc1`).

```terminal
git tag v0.12.0-rc1
git push origin v0.12.0-rc1
```

The CD pipeline detects RC tags via the `-` in the tag name and skips the steps that should
only run for a final release:

| Step                            | Final tag | RC tag    |
|---------------------------------|-----------|-----------|
| Native images (Linux/macOS/Win) | ✓         | ✓         |
| GitHub release (pre-release)    | ✓         | ✓         |
| Docker image `:<version>` tag   | ✓         | ✓         |
| Gradle plugin publication       | ✓         | ✓         |
| Docker image `:latest` tag      | ✓         | *skipped* |
| Website and docs deploy         | ✓         | *skipped* |
| GitHub Discussions announcement | ✓         | *skipped* |

When the manual tests against the RC artifacts pass, tag the *same commit* with the final version
and push it — the pipeline runs again and performs the steps that were skipped for the RC:

```terminal
git tag v0.12.0
git push origin v0.12.0
```

If an RC reveals a problem, fix it on `main`, then push a fresh RC tag (`v0.12.0-rc2`, …).
RC tags can be safely deleted from the remote once the final tag is published.

## Documentation and Antora versioning

Documentation changes for the next release are prepared on a dedicated branch (e.g. `docs/0.12.0`)
created from `main`. This keeps doc work separate from feature PRs and avoids accidental deploys.

### Workflow

1. **Create the docs branch** from `main`:
   ```terminal
   git branch docs/0.12.0 main
   ```
   A git worktree is convenient for working on the docs branch alongside `main`:
   ```terminal
   git worktree add ../vulnlog-docs docs/0.12.0
   ```

2. **On the docs branch**, bump `version` in `docs/antora.yml` to the upcoming release
   and make all Antora documentation changes there.

3. **At release time**, create a PR from the docs branch into `main` and merge it.
   Then tag the release on `main` as described above.

4. **After tagging**, create a release branch to preserve the docs for that version:
   ```terminal
   git branch release/0.12.0 v0.12.0
   git push origin release/0.12.0
   ```

The website and docs site will be deployed automatically from the CD pipeline.

### Manual deployment

Whenever a manual deployment is required from the current version of the main branch:

1. **Deploy the website** by manually triggering the `deploy-website.yml` workflow
   from the GitHub Actions UI (on `main`).

2. **Quick doc fixes** on `main` can be deployed the same way — push the fix and
   manually trigger the workflow.

The `deploy-website.yml` workflow is manual-only (`workflow_dispatch`), so the site
is never deployed without an explicit trigger.

## Maintenance releases (vl-0.9 branch)

For patches to the previous CLI, work on the `vl-0.9` branch and tag from there.
The pipeline triggers on tags matching `v0.9.*` (e.g. `v0.9.5`).

```terminal
git checkout vl-0.9
# ... make fixes, commit ...
git tag v0.9.5
git push origin v0.9.5
```
