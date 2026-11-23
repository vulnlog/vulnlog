# Contributing to Vulnlog

Thank you for contributing. Every contribution helps, whether it's a bug report, a docs fix, or a new feature.

## Where to start

Check the open [issues](https://github.com/vulnlog/vulnlog/issues) for tasks. Look for issues labelled _help wanted_.
If you're new to the project, start with issues labelled _good first issue_.

## Reporting bugs

File bugs in [GitHub Issues](https://github.com/vulnlog/vulnlog/issues/new) and include:

- What you were trying to achieve and the impact.
- The output of `vulnlog --version`.
- The complete error message and steps to reproduce.

## Proposing changes

Before opening a pull request for a significant change, check existing
[issues](https://github.com/vulnlog/vulnlog/issues) and
[discussions](https://github.com/vulnlog/vulnlog/discussions) to avoid duplication. Open a new issue or discussion to
propose the idea.

## Contributing code

The high-level steps:

1. Fork and clone
2. Build the project
3. Make changes, test, commit, and push
4. Open a pull request

Prerequisites:

* `git` (verify with `git --version`)
* JDK 17+ to run Gradle (verify with `java --version`)

This project is licensed under the [Apache License, Version 2.0](LICENSE). By contributing, you agree that your
contributions are licensed under the same terms.

### Step 1: Fork and clone

* Fork the Vulnlog project to your namespace: [Fork Vulnlog](https://github.com/vulnlog/vulnlog/fork)
* Clone the fork. Replace `<your-namespace>` with the GitHub account name you forked the project to.
  ```terminal
  git clone git@github.com:<your-namespace>/vulnlog.git
  ```
* Change into the new directory: `cd vulnlog`

### Step 2: Build the project

Vulnlog is a Kotlin project built with Gradle. The repository includes the Gradle wrapper (`gradlew`).

* Build the project from the repository root:
  ```terminal
  ./gradlew build
  ```
  This downloads dependencies and runs the test suite.
* Run the application with Gradle:
  ```terminal
  ./gradlew run --args='--version'
  ```
* Or build and run a standalone CLI:
  ```terminal
  ./gradlew installDist
  ./modules/cli-app/build/install/vulnlog/bin/vulnlog --version
  ```

#### Build the native binary

You only need the native binary to test the native image locally or to build the Docker image. The JVM build above
covers everything else.

* Install [Oracle GraalVM 25](https://www.graalvm.org/downloads/), the distribution the CI uses, and point `JAVA_HOME`
  to it. Gradle doesn't download GraalVM for you.
* Install the C toolchain that Native Image needs to link the binary:
    * Debian and Ubuntu:
      ```terminal
      sudo apt-get install build-essential zlib1g-dev
      ```
    * Fedora, RHEL, and other distributions with `dnf`:
      ```terminal
      sudo dnf install gcc glibc-devel zlib-devel libstdc++-static
      ```
    * macOS: `xcode-select --install`
    * Windows: Visual Studio 2022 17.13 or later with the "Desktop development with C++" workload. Native Image finds
      the installation by itself, so a normal PowerShell or Command Prompt works.
* Build the native binary. Native Build Tools doesn't support the configuration cache yet, so turn it off:
  ```terminal
  ./gradlew :cli-app:nativeCompile --no-configuration-cache
  ```
  On Windows, use `gradlew.bat` instead of `./gradlew`.
* Run it from the build directory:
  ```terminal
  ./modules/cli-app/build/native/nativeCompile/vulnlog --version
  ```
  On Windows, the binary is `vulnlog.exe`.

If the build fails with `cannot find -lz`, the zlib development files are missing. Install them as shown above.

### Step 3: Make changes, test, commit, and push

* Make the change in the project. See the [Developer Documentation](devdoc/) for internals,
  or [Building the documentation](#building-the-documentation) for changes to the user-facing docs.
* Format the code and run all checks before committing:
  ```terminal
  ./gradlew spotlessApply
  ./gradlew ktlintFormat
  ./gradlew check
  ```
* Commit the changes following the [Commit Conventions](#commit-conventions) below, including a DCO sign-off.
* Push to the fork:
  ```terminal
  git push
  ```

### Step 4: Open a pull request

Open a pull request against the `main` branch of `vulnlog/vulnlog`.

## Building the documentation

The user-facing docs are written in AsciiDoc under `docs/modules/ROOT/pages/` and built
with [Antora](https://antora.org). The Gradle wrapper provisions Node.js automatically, so no separate Node install is
required. `./gradlew docsBuild` runs `npm ci` first, which installs Antora and the search extension in the versions
pinned in `package-lock.json`. The Antora UI is the bundle in `docs/ui`. Dependabot updates them, and CI builds the site
for every such update.

`./gradlew docsBuild` runs Antora against `antora-local-playbook.yml`, which reads the docs of the branch you have
checked out, with your uncommitted changes. It also works in a linked Git worktree.

* Build the docs:
  ```terminal
  ./gradlew docsBuild
  ```
  The build writes the output to `build/docs/`.
* Preview by opening `build/docs/index.html` in a browser.
* After editing an `.adoc` file, rerun `./gradlew docsBuild` and refresh the browser.

CI builds the production site from `antora-playbook.yml`. Local builds don't affect it.

## Commit conventions

This project uses [Conventional Commits](https://www.conventionalcommits.org). Every commit message must follow this
format:

```
<type>: <description>
```

Keep subject lines to 50 characters or fewer, use the imperative mood ("Add support for..." not "Added support for..."),
and don't end with a period. Use the commit body to explain *what* and *why*, not *how*.

| Type       | Purpose                                               |
|------------|-------------------------------------------------------|
| `feat`     | New feature or capability                             |
| `fix`      | Bug fix                                               |
| `docs`     | Documentation changes                                 |
| `refactor` | Code restructuring without behaviour change           |
| `perf`     | Performance improvement                               |
| `test`     | Adding or updating tests                              |
| `ci`       | CI/CD configuration and dependency updates            |
| `chore`    | Maintenance tasks (dependency updates, configs, etc.) |

### Developer Certificate of Origin

This project uses the [Developer Certificate of Origin](https://developercertificate.org/) (DCO) to certify that
contributors have the right to submit their work under the project's licence. Every commit must include a
`Signed-off-by` trailer with the contributor's real name and email address.

Use Git's `-s` / `--signoff` flag:

```terminal
git commit -s -m "fix: Correct YAML parsing for empty lists"
```

To sign off the most recent commit:

```terminal
git commit --amend -s --no-edit
```

Commits without a valid `Signed-off-by` trailer fail the CI check.
