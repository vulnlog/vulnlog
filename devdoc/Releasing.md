# Releasing

## Developer builds

Running or building locally without passing a version produces a developer build automatically:

```terminal
./gradlew :cli-app:run
# prints: Vulnlog SNAPSHOT+a1b2c3d
```

The version is `SNAPSHOT+<git-short-hash>`, so it is always traceable to a specific commit.

## How the release pipeline works

Releases are driven by version tags. Pushing a tag matching `v[0-9]+.[0-9]+.[0-9]+` (final) or
`v[0-9]+.[0-9]+.[0-9]+-rc[0-9]+` (release candidate) starts the
[`release.yaml`](../.github/workflows/release.yaml) pipeline. It publishes in stages, ordered
so that nothing irreversible happens while an earlier stage can still fail:

1. **Gate**: `./gradlew check` and the three native images (Linux, macOS, Windows) build in
   parallel. If anything fails, nothing is published.
2. **GitHub release**: created as pre-release with the JVM distribution zip, the three native
   image zips, and the install scripts attached. A GitHub release can still be edited or
   deleted if something is wrong.
3. **Docker image, website deploy, and Homebrew formula bump** run in parallel.
4. **Gradle plugin**: published to the Gradle Plugin Portal last, because a Portal publication
   cannot be retracted.
5. **Announcement**: a GitHub Discussions post, once everything else is out.

The pipeline compares the pushed tag against all existing version tags. Only the highest
version ("latest") deploys the website, moves the Docker `:latest` tag, and bumps the
Homebrew formula. A bug fix release for an older minor (for example `v0.15.1` after `v0.16.0`
is out) skips those stages automatically; a bug fix for the current minor runs them.

| Stage                           | Latest final | Patch of older minor | RC tag  |
|---------------------------------|--------------|----------------------|---------|
| Check and native images (gate)  | yes          | yes                  | yes     |
| GitHub release (pre-release)    | yes          | yes                  | yes     |
| Docker image `:<version>`       | yes          | yes                  | yes     |
| Docker image `:latest`          | yes          | skipped              | skipped |
| Website and docs deploy         | yes          | skipped              | skipped |
| Homebrew formula bump dispatch  | yes          | skipped              | skipped |
| Gradle plugin publication       | yes          | yes                  | skipped |
| GitHub Discussions announcement | yes          | yes                  | skipped |

The pipeline never writes to the repository: `CHANGELOG.md` and the docs version are prepared
by the maintainer in a release preparation PR before tagging.

## Branch model

- **`main`**: all development, including documentation for the next release.
- **`release/<major.minor>`** (for example `release/0.16`): created from the release commit
  when `v<major.minor>.0` is cut. Holds everything related to that release line: the published
  docs (Antora sources them from the branches listed in
  [`antora-playbook.yml`](../antora-playbook.yml)) and the cherry-picked fixes for patch
  releases.

## Feature release (vX.Y.0)

### 1. Open the release preparation PR

All changes to `main` go through PRs, including release preparation.

- Create a branch from `main`, for example `release-prep/0.16.0`.
- Bump `version` and `vulnlog-version` in `docs/antora.yml` to the new version.
- Add the upcoming `release/0.16` branch to the `branches` list in `antora-playbook.yml`
  (and drop the oldest entry if its docs should no longer be published).
- Update the changelog. The new version is passed with `--tag` because the git tag does not
  exist yet:

  ```terminal
  git-cliff --config .github/changelog.toml \
    --unreleased --tag v0.16.0 \
    --github-token "$(gh auth token)" \
    --prepend CHANGELOG.md
  ```

- Add the compare link for the new version at the top of the link block at the bottom of
  `CHANGELOG.md`, following the existing convention (label without the `v` prefix, URL tags
  with it):

  ```
  [0.16.0]: https://github.com/vulnlog/vulnlog/compare/v0.15.1...v0.16.0
  ```

- `git-cliff` renders squashed PR titles verbatim, so fix any typos in the new section (see
  [Pull Requests](Pull-Requests.md) for the title conventions).
- Open the PR, get it green, and merge it.

### 2. Optional: validate with a release candidate

To validate the final binaries before cutting the release, tag the merge commit with an RC
version first:

```terminal
git checkout main && git pull
git tag v0.16.0-rc1
git push origin v0.16.0-rc1
```

- The pipeline publishes a GitHub pre-release with all binaries and a Docker `:0.16.0-rc1`
  image; everything else is skipped (see the table above).
- Test the RC artifacts manually.
- If the RC reveals a problem, fix it on `main` through a PR and push a fresh RC tag
  (`v0.16.0-rc2`, and so on).
- RC tags and their GitHub pre-releases can be deleted once the final release is out.

### 3. Create the release branch, then tag

The release branch must exist before the tag is pushed, because the website deploy builds the
docs from the branches listed in `antora-playbook.yml`.

```terminal
git checkout main && git pull
git branch release/0.16
git push origin release/0.16
git tag v0.16.0
git push origin v0.16.0
```

The pipeline runs all stages, including the ones skipped for the RC.

### 4. Finish the GitHub release

The release lands as pre-release. Its body contains GitHub's auto-generated "What's Changed"
list plus a link to `CHANGELOG.md`.

- Review the body in the GitHub UI and refine it: fix typos, add highlights, link to docs.
- For a richer draft grouped by conventional-commit type, generate it locally and paste it in:

  ```terminal
  git-cliff --config .github/release-notes.toml \
    --latest --github-token "$(gh auth token)"
  ```

- Un-flag the pre-release toggle to publish the release.

### 5. After the release

- Check that the Homebrew tap opened and merged its bump PR in
  [vulnlog/homebrew-vulnlog](https://github.com/vulnlog/homebrew-vulnlog).
- Spot-check https://vulnlog.dev, the Docker `:latest` tag, and the
  [Gradle Plugin Portal](https://plugins.gradle.org/plugin/dev.vulnlog) listing.

## Bug fix release (vX.Y.Z)

Fixes always land on `main` first (through normal PRs), then reach the release line by
cherry-pick. The tag is pushed on the release branch.

### 1. Open the patch preparation PR against the release branch

- Create a branch from `release/0.15`, for example `release-prep/0.15.1`.
- Cherry-pick the fix commits from `main`.
- Bump `vulnlog-version` in `docs/antora.yml` to `0.15.1` (leave `version` at `0.15`).
- Prepend the changelog section, run from this branch so git-cliff picks up the cherry-picked
  commits since `v0.15.0`:

  ```terminal
  git-cliff --config .github/changelog.toml \
    --unreleased --tag v0.15.1 \
    --github-token "$(gh auth token)" \
    --prepend CHANGELOG.md
  ```

- Add the compare link (`[0.15.1]: .../compare/v0.15.0...v0.15.1`) and fix typos as for a
  feature release.
- Open the PR against `release/0.15` and merge it.

### 2. Tag the patch release

```terminal
git checkout release/0.15 && git pull
git tag v0.15.1
git push origin v0.15.1
```

An RC first (`v0.15.1-rc1`) works here too, if the patch warrants validation.

The pipeline detects automatically whether `v0.15.1` is the highest version overall and skips
the website deploy, the Docker `:latest` tag, and the Homebrew bump when it is not (see the
table above). The GitHub release, the versioned Docker image, the Gradle plugin, and the
announcement are published either way.

### 3. Port the changelog to main

`CHANGELOG.md` on `main` is the complete record of every release. Add the same `0.15.1`
section and compare link there through a small PR.

Then finish the GitHub release as described in the feature release steps.

## Documentation

The published docs at https://vulnlog.dev/docs are built from the `release/*` branches listed
in `antora-playbook.yml`; `main` is not a docs source. Docs for the next release are written
on `main` and go live when the next release branch is created.

To fix the live docs between releases:

- Fix them on `main` through a normal PR.
- Cherry-pick the fix to the newest release branch through a PR.
- Trigger the [`deploy-website.yml`](../.github/workflows/deploy-website.yml) workflow
  manually from the Actions tab.

## Manual website deployment

The website and docs deploy automatically on releases of the latest version and refresh
weekly after the security scan. For anything in between, trigger the
[`deploy-website.yml`](../.github/workflows/deploy-website.yml) workflow manually from the
Actions tab.
