# Releasing

Releases are automated. Tag a commit with the semantic version tag (e.g. v1.2.3) and push it to the main branch.
The [CD pipeline](../.github/workflows/cd.yaml) will then create and publish the artefacts, and prepare a GitHub release
in draft form. Once you are happy with it, publish the release manually from the GitHub UI.

```terminal
git tag v1.2.3
git push
```
