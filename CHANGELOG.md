# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Gradle Plugin: Support to only generate HTML reports for specified release branches by `releaseBranch.addAll()` in the
  `vulnlog` Gradle plugin extension.
- Add advanced filtering options in the HTML report to only show relevant information.

## [0.7.2] - 2025-05-08

### Fixed

- Fix missing task information in the HTML report overview.
- Improve and fix the logo in the HTML report.

## [0.7.1] - 2025-04-02

### Changed

- DSL: The `temporarily` specifier no longer requires an include statement in a Vulnlog file.
- Make the HTML report self-contained and independent of internet resources.

### Fixed

- Fix parsing of JSON data when task data is undefined

## [0.7.0] - 2025-04-19

### Added

- HTML Report: Add affected and fixed version information in the HTML report.
- DSL: Introduce `fixedAt` statement to define a vulnerability report as fixed.
- HTML Report: Add vulnerability report status per entry (`under investigation`, `not affected`, `affected`, `fixed` and
  `unknown`)
- DSL: Support multiple reporters in `from` statement.
- Improved the design and table functionality in the HTML report.

## [0.6.0] - 2025-03-15

### Added

- Introduce `reporters` and `reporter` DSL to define a custom reporter. The existing `SCA_SCANNER` is deprecated and
  will
  be removed in an upcoming release.
- Introduce verdict types (`notAffected`, `low`, `moderate`, `high` and `critical`) to the DSL. The existing
  string-based verdict is deprecated and will be removed in an upcoming release.
- More DSL API documentation.

## [0.5.3] - 2025-03-12

### Change

- CI: Fix CI publishing pipeline.

## [0.5.2] - 2025-03-12

### Change

- CI: Split CI publishing pipeline.

## [0.5.1] - 2025-03-12

### Change

- CI: Add Gradle Vulnlog publishing to the CI pipeline.

## [0.5.0] - 2025-03-10

### Added

- CLI: Simple HTML report generation with subcommand _report_.
- Gradle Plugin: New task to print the CLI version (`showCliVersion`) and generate reports (`generateReport`).

### Changed

- CLI: Default output (without subcommand) is now the JSON structure.
- DSL: Rework execution syntax

## [0.4.0] - 2025-02-21

### Added

- CLI: Improve Vulnlog file parsing by using the file compilation cache. The first time a new or changed Vulnlog file is
  processed by the `vl` command, it is compiled and cached. Subsequent processing will use the cached file. If the
  Vulnlog file is changed, `vl` will detect this and recompile and cache the file.
- DSL: Completely revised and simplified DSL for describing releases and vulnerabilities. The new DSL also supports
  multiple files to separate release definitions from vulnerability analysis, reducing noise in the Vulnlog file.

### Removed

- Previous DSL versions.

## [0.3.3] - 2024-12-21

### Added

- Fix CI pipeline.
- Upload release artifact to webserver.

## [0.3.2] - 2024-12-21

### Added

- Fix API documentation publishing to webserver.
- Set CLI binary name to vl.

## [0.3.1] - 2024-12-20

### Added

- New DSL API documentation releases.

## [0.3.0] - 2024-12-16

### Added

- Reduced DSL artefact size by moving implementation classes to the DSL interpreting package.
- DSL API web publication to https://vulnlog.dev/dslapi/SNAPSHOT and https://vulnlog.dev/dslapi/latest by the CI
  pipeline.
- Add MVP CLI application to parse simple Vulnlog files and print information to STDOUT. Also, simple filter flags are
  supported.
- Add GitHub Action publishing pipeline for CLI application and DSL JAR package.

## [0.2.0] - 2024-11-24

### Added

- Initial vulnerability DSL.
