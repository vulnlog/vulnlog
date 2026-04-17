# Releasing

## Developer builds

Running or building locally without passing a version produces a developer build automatically:

```terminal
./gradlew :cli-app:run
# → Vulnlog SNAPSHOT+a1b2c3d
```

The version is `SNAPSHOT+<git-short-hash>`, so it is always traceable to a specific commit.

## Release builds (next CLI)

Releases are automated via the [CD pipeline](../.github/workflows/cd.yaml).
Tag a commit on `main` with the version (without the `v` prefix in the build, but with it in the tag),
then push the tag. The pipeline triggers on tags matching `v[0-9]+.[0-9]+.[0-9]+`
(e.g. `v0.10.0`, `v0.11.2`, `v1.0.0`); maintenance tags `v0.9.*` are excluded and handled by
[`cd-0.9.yaml`](../.github/workflows/cd-0.9.yaml).

```terminal
git tag v0.12.0
git push origin v0.12.0
```

The pipeline will:

1. Generate and commit an updated `CHANGELOG.md` to `main`
2. Build native images for Linux, macOS, and Windows in parallel
3. Build the JVM distribution zip
4. Create a GitHub release (marked as pre-release) with all four artifacts attached
5. Publish the Gradle plugin to the Gradle Plugin Portal
6. Push the Docker image to `ghcr.io` with both `:<version>` and `:latest` tags
7. Deploy the website and Antora docs to GitHub Pages
8. Post a release announcement in GitHub Discussions

Once you are satisfied with the release notes, publish the release manually from the GitHub UI
to remove the pre-release flag.

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
| `CHANGELOG.md` commit to `main` | ✓         | *skipped* |
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

Whenever a manuall depolyment is required from the current version of the main branch:

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
