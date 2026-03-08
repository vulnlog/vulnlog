# Releasing

## Developer builds

Running or building locally without passing a version produces a developer build automatically:

```terminal
./gradlew :next:run
# → Vulnlog SNAPSHOT+a1b2c3d
```

The version is `SNAPSHOT+<git-short-hash>`, so it is always traceable to a specific commit.

## Release builds (next CLI)

Releases are automated via the [CD pipeline](../.github/workflows/cd.yaml).
Tag a commit on `main` with the version (without the `v` prefix in the build, but with it in the tag),
then push the tag. The pipeline triggers on tags matching `v0.1[0-9].*` (e.g. `v0.10.0`, `v0.11.2`).

```terminal
git tag v0.10.0
git push origin v0.10.0
```

The pipeline will:
1. Generate and commit an updated `CHANGELOG.md` to `main`
2. Build native images for Linux, macOS, and Windows in parallel
3. Build the JVM distribution zip
4. Create a GitHub release (marked as pre-release) with all four artifacts attached
5. Post a release announcement in GitHub Discussions

Once you are satisfied with the release notes, publish the release manually from the GitHub UI
to remove the pre-release flag.

## Maintenance releases (vl-0.9 branch)

For patches to the previous CLI, work on the `vl-0.9` branch and tag from there.
The pipeline triggers on tags matching `v0.9.*` (e.g. `v0.9.5`).

```terminal
git checkout vl-0.9
# ... make fixes, commit ...
git tag v0.9.5
git push origin v0.9.5
```
