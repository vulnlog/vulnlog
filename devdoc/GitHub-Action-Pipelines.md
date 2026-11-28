# GitHub Actions pipelines

The workflow files in `.github/workflows/` are the source of truth. This page explains why they work the way they do,
and what you keep up by hand: repository settings, pinned versions, and conventions.

## Workflows

| Workflow                                                              | Runs on                                                    | Does                                                    |
|-----------------------------------------------------------------------|------------------------------------------------------------|---------------------------------------------------------|
| [`ci.yaml`](../.github/workflows/ci.yaml)                             | Pull requests, pushes to `main`, manual                    | Checks the build, builds the docs and the native image  |
| [`pr-title.yaml`](../.github/workflows/pr-title.yaml)                 | Pull requests, also when you edit the title                | Checks the conventional commit prefix of the PR title   |
| [`lint-workflows.yaml`](../.github/workflows/lint-workflows.yaml)     | Pull requests and pushes to `main` that change `.github/`  | Runs actionlint and zizmor                              |
| [`codeql.yaml`](../.github/workflows/codeql.yaml)                     | Pull requests, pushes to `main`, weekly                    | Analyses the Kotlin code                                |
| [`release.yaml`](../.github/workflows/release.yaml)                   | Version tags                                               | Publishes a release                                     |
| [`deploy-website.yml`](../.github/workflows/deploy-website.yml)       | Releases, manual, after the security scan, `.vex/` changes | Deploys https://vulnlog.dev                             |
| [`deploy-stage.yaml`](../.github/workflows/deploy-stage.yaml)         | Pushes to `main` that change the site, manual              | Deploys `main` to https://stage.vulnlog.dev             |
| [`security.yml`](../.github/workflows/security.yml)                   | Weekly, manual                                             | Scans with Snyk and Trivy                               |
| [`dependency-graph.yaml`](../.github/workflows/dependency-graph.yaml) | Pushes to `main`                                           | Submits the Gradle dependencies to GitHub               |

The workflows share these parts:

- [`build-native.yaml`](../.github/workflows/build-native.yaml) builds the native image. CI and the release call it.
- [`setup-jvm`](../.github/actions/setup-jvm/action.yml) installs Java 21 to run Gradle. The build itself compiles
  with the Java 25 toolchain.
- [`build-docs`](../.github/actions/build-docs/action.yml) builds the docs site and fails if the search is missing.
- [`fetch-native-binary`](../.github/actions/fetch-native-binary/action.yml) downloads the native binary of an
  earlier job.

## CI

CI never publishes anything.

- `ci-ok` is the required status check. It fails when another CI job failed or was cancelled. Skipped jobs count as
  passed. You can add or rename CI jobs without touching the branch rules.
- On pull requests, `native-image` and `docs` run only when the PR changes relevant files. The `detect-changes` job
  lists them.
- On pull requests, `docs` builds the PR's own docs with `antora-local-playbook.yml` and fails on errors such as a
  broken xref. On `main`, it builds the published site from the release branches with `antora-playbook.yml`.
- The DCO check skips Dependabot PRs, because bot commits carry no sign-off.
- `perf-test` fails when a command's mean runtime exceeds 1000 ms. To run it on demand, start CI manually. To try
  another threshold, run `perf/perf-test.sh` locally with `THRESHOLD_MS` set.
- `./gradlew check` also runs `checkVexDocuments`, which fails when `.vex/` doesn't match `vulnlog.yaml`.

## Release

A pushed version tag starts the release. See [Releasing](Releasing.md) for the steps. The stages run so that the
ones that are hardest to undo come last:

1. Gate: `./gradlew check :buildSrc:check` and the four native images. If one fails, nothing is published.
2. GitHub release, as pre-release, with the binaries, SBOMs, install scripts, `SHA256SUMS`, and their attestations.
   You can still edit or delete a release, but not an attestation.
3. Docker image, website deploy, and Homebrew bump, in parallel. The Docker stage adds the container SBOM to the
   release.
4. Gradle plugin, last, because you can't take back a Gradle Plugin Portal publication.
5. Announcement in GitHub Discussions.

The `version` job decides what a tag publishes. "Latest" means the highest final version tag.

| Stage                         | Latest final | Patch of an older minor | Release candidate |
|-------------------------------|--------------|-------------------------|-------------------|
| Gate and GitHub pre-release   | yes          | yes                     | yes               |
| Docker image `:<version>`     | yes          | yes                     | yes               |
| Docker image `:latest`        | yes          | no                      | no                |
| Website deploy, Homebrew bump | yes          | no                      | no                |
| Gradle plugin, announcement   | yes          | yes                     | no                |

The release never writes to the repository. You prepare `CHANGELOG.md` and the docs version in a PR before you tag.

The container SBOM combines what Syft finds in the image with the native image SBOM, because Syft can't read the
dependencies inside a native binary. The image attestations go to GHCR as OCI referrers, so the version tag stays a
single manifest.

## Website deploy

The site always shows the latest release, whatever started the deploy:

- The version is the highest final version tag, or the input of the release.
- `website/` and `schema/` come from the release branch of that version, like the docs. `main` may already hold
  changes for the next release.
- The install scripts are the assets of the GitHub release, so the site serves the files that `SHA256SUMS` lists.
- `.vex/` comes from the commit that started the deploy: `main`, or the tag of a release.
- The security reports come from the last successful scan on `main`.

Every deploy lists the files in the storage zone that the site no longer has. It deletes them only when you run it
manually with `delete-stale`, because the zone may hold files the workflow never uploaded.

The staging site shows `main`, so you can review the site of the next release before it ships. `deploy-stage.yaml`
calls the same deploy with `stage`, which changes four things:

- `website/`, `schema/`, and the docs come from `main`. The docs use `antora-local-playbook.yml`.
- The install scripts stay off staging, so no untested install script runs from the vulnlog.dev domain.
- The deploy deletes stale files without asking, because the workflow owns the staging zone.
- The upload uses the `stage` environment and its secrets.

The deploy doesn't purge the CDN cache. The purge API needs the bunny.net account API key, which has full access to
the account, so it stays out of CI. Keep the cache time of HTML, JSON, and the install scripts short in the pull
zone settings.

## Scans

- `security.yml` installs the latest Vulnlog release, generates the suppression files from `vulnlog.yaml`, and runs
  Snyk and Trivy with them. Trivy scans the jars built from `main` and the image `:latest`. The website shows the
  reports of the last scan.
- `codeql.yaml` compiles with `--no-build-cache`, because CodeQL analyses only the code a build compiles.
- `dependency-graph.yaml` gives the Gradle dependencies to the dependency graph and the Dependabot alerts. GitHub
  doesn't read Gradle builds by itself.

All results go to code scanning in the Security tab.

## Conventions

zizmor checks most of these on every change to `.github/`:

- Pin every action to a full commit SHA with the version as a comment. Dependabot bumps both.
- Pass values to `run` blocks through `env`, never as `${{ }}`, so no value can turn into shell code.
- Check out with `persist-credentials: false`. No job pushes.
- Keep the permissions of each workflow read-only and let each job ask for more.
- When a zizmor finding is safe, add `# zizmor: ignore[<audit>]` with the reason next to it.

Dependabot doesn't bump these versions, so bump them yourself now and then:

- actionlint in `lint-workflows.yaml`
- zizmor in `lint-workflows.yaml`. A new release can add audits, so expect new findings.

## Repository settings

Part of the security lives in the repository settings. Keep them as listed here.

Environments hold the publishing secrets. A job gets them only when it names the environment and runs on an allowed
branch or tag. The website deploy is a reusable workflow, so it sees only the secrets of its environment, never the
repository secrets:

| Environment | Secrets                                                                | Branches and tags        |
|-------------|------------------------------------------------------------------------|--------------------------|
| `website`   | `BUNNY_ACCESS_KEY`, `BUNNY_STORAGE_ZONE_REGION_ENDPOINT`               | Branch `main`, tags `v*` |
| `release`   | `GRADLE_PLUGIN_KEY`, `GRADLE_PLUGIN_SECRET`, `HOMEBREW_DISPATCH_TOKEN` | Tags `v*`                |
| `stage`     | `BUNNY_STAGE_ACCESS_KEY`, `BUNNY_STAGE_STORAGE_ZONE_REGION_ENDPOINT`   | Branch `main`            |

`SNYK_API_KEY` is the only repository secret.

Rulesets:

- `main` and `release/*` require a pull request and the status checks `ci-ok` and `PR title`.
- Only maintainers can create, update, or delete `v*` tags. A tag starts a release, so this decides who can publish.

Actions:

- Require actions to be pinned to a full-length commit SHA.
- Give `GITHUB_TOKEN` read permissions only by default.
- Keep automatic dependency submission off for Gradle, so the Gradle dependencies aren't submitted twice.

Bunny:

- The staging pull zone sends `X-Robots-Tag: noindex` through an edge rule, so search engines skip staging. Staging
  serves the same files as production, so a `robots.txt` in the repository can't do this.
- The staging pull zone caches only briefly, so a push shows within seconds.
