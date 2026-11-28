# Releasing

You release by pushing a version tag. [GitHub Actions pipelines](GitHub-Action-Pipelines.md#release) explains what
each tag publishes.

## Branches

- `main` holds all development, including the docs of the next release.
- `release/<major.minor>`, for example `release/0.19`, starts at the release commit of `v<major.minor>.0`. It holds
  the published docs and website of that release line and the fixes for its patch releases.

## Developer builds

A build without a version gets `SNAPSHOT+<git short hash>`, so you can trace it to its commit:

```terminal
./gradlew :cli-app:run
```

## Release a minor version

The examples release `v0.20.0`.

### 1. Prepare the release

Create the preparation branch:

```terminal
git switch main
git pull
git switch -c release-prep/0.20.0
```

Then:

- In `docs/antora.yml`, set `version` to `0.20` and `vulnlog-version` to `0.20.0`.
- In `antora-playbook.yml`, add `release/0.20` to the branches. Remove the oldest one if its docs shouldn't stay
  online.
- Add the changelog section. The tag doesn't exist yet, so pass it with `--tag`:

  ```terminal
  git-cliff --config .github/changelog.toml \
    --unreleased --tag v0.20.0 \
    --github-token "$(gh auth token)" \
    --prepend CHANGELOG.md
  ```

- Add the compare link at the top of the link block at the end of `CHANGELOG.md`:

  ```
  [0.20.0]: https://github.com/vulnlog/vulnlog/compare/v0.19.0...v0.20.0
  ```

- git-cliff copies the PR titles as they are, so fix typos in the new section. See [Pull Requests](Pull-Requests.md).

Open the PR and merge it.

### 2. Optionally, test a release candidate

A release candidate publishes the binaries and a Docker image `:0.20.0-rc1`, and nothing else:

```terminal
git switch main
git pull
git tag v0.20.0-rc1
git push origin v0.20.0-rc1
```

Test the artifacts. If you find a problem, fix it on `main` through a PR and tag `v0.20.0-rc2`. Delete the release
candidates once the final release is out.

### 3. Create the release branch and tag

Create the release branch first. The website deploy reads the docs and the website from it.

```terminal
git switch main
git pull
git switch -c release/0.20
git push origin release/0.20
git tag v0.20.0
git push origin v0.20.0
```

### 4. Publish the GitHub release

The release starts as pre-release. Its body has GitHub's generated list of changes and a link to `CHANGELOG.md`.

- Edit the body: fix typos, add highlights, and link to the docs. For notes grouped by commit type, run
  `git-cliff --config .github/release-notes.toml --latest --github-token "$(gh auth token)"` and paste the output.
- Untick "Set as a pre-release". For a patch of an older minor version, also untick "Set as the latest release".

### 5. Check the release

- The Homebrew tap opened and merged its bump PR in
  [vulnlog/homebrew-vulnlog](https://github.com/vulnlog/homebrew-vulnlog).
- https://vulnlog.dev shows the new version.
- The Docker image `:latest` and the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/dev.vulnlog.plugin)
  have the new version.

## Release a patch version

The examples release `v0.19.1`. Fixes land on `main` first and reach the release branch by cherry-pick.

### 1. Prepare the patch

```terminal
git switch release/0.19
git pull
git switch -c release-prep/0.19.1
git cherry-pick <commit>
```

Then:

- In `docs/antora.yml`, set `vulnlog-version` to `0.19.1`. Leave `version` at `0.19`.
- Add the changelog section from this branch, so git-cliff sees the cherry-picked commits:

  ```terminal
  git-cliff --config .github/changelog.toml \
    --unreleased --tag v0.19.1 \
    --github-token "$(gh auth token)" \
    --prepend CHANGELOG.md
  ```

- Add the compare link `[0.19.1]: https://github.com/vulnlog/vulnlog/compare/v0.19.0...v0.19.1` and fix typos.

Open the PR against `release/0.19` and merge it.

### 2. Tag the patch

```terminal
git switch release/0.19
git pull
git tag v0.19.1
git push origin v0.19.1
```

A release candidate, `v0.19.1-rc1`, works here too.

### 3. Finish

- Publish the GitHub release as in step 4 of a minor version.
- `CHANGELOG.md` on `main` records every release. Add the same section and compare link there through a PR.

## Fix the live docs or website

The live docs, website, and JSON schemas come from the release branch of the latest release.

1. Fix it on `main` through a PR. Once merged, check the result on https://stage.vulnlog.dev.
2. Cherry-pick the fix to the newest release branch through a PR.
3. Run [`deploy-website.yml`](../.github/workflows/deploy-website.yml) manually in the Actions tab.

The install scripts come from the assets of the latest release, so a change to them goes live with the next release.

## Delete stale files from the website

Every website deploy lists the files in Bunny Storage that the site no longer has. Check that list in the log of the
last deploy. If you want them gone, run `deploy-website.yml` manually with `delete-stale` ticked.
