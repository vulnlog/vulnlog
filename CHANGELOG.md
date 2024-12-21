# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.3.2] - 2024-12-21

### Added

- Fix API documentation publishing to webserver.
- Set CLI binary name to vl.

## [0.3.1] - 2024-12-20

### Added

- New DSL API documentation releases.

## [0.3.0] - 2024-12-16

### Added

- Reduced DSL artefact size by moving implementation classes to DSL interpreting package.
- DSL API web publication to https://vulnlog.dev/dslapi/SNAPSHOT and https://vulnlog.dev/dslapi/latest by the CI
  pipeline.
- Add MVP CLI application to parse simple Vulnlog files and print information to STDOUT. Also, simple filter flags are
  supported.
- Add GitHub Action publishing pipeline for CLI application and DSL JAR package.

## [0.2.0] - 2024-11-24

### Added

- Initial vulnerability DSL.
