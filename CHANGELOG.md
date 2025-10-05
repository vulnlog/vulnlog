# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.9.1-test1] - 2025-10-05

### Added

- Add Github action workflow to deploy project website to Github pages
- Add project website files to mono repo
- Add comparison links to CHANGELOG.md

### Changed

- Start next release
- Bump Gradle to version 9.1.0
- Move project files into new directory to create a mono repo
- Fix deploy-website workflow to set working directory and cache dependency path
- Use own webserver instead of github pages
- Trigger website deployment on changes
- Rework version and releaseing
- Bump cliff Github action to version 4
- Fix working directory for gradle runs
- Use example config for cliff's keep-a-changelog format
- Add `contents: write` permission to release workflow

## [0.9.0] - 2025-06-29

### Added

- Add reporter suppression DSL configuration
- Add suppression module and refactor common vulnerability data
- Add support to write generated suppression files to STDOUT or to a file
- Add a suppression task and options to the Gradle plugin
- Add help text for the suppress and report command
- Add branch service to abstract the branch repository and apply branch filtering

### Changed

- Start next release
- Refactor DSL interfaces and implementation for consistency
- Refactor suppression logic to use VulnPerBranchAndRecord
- Refactor to rename and restructure vulnerability records
- Filter vulnerabilities by reporter ID matcher
- Fixup refactor suppression logic to use
- Filter and collect relevant vulnerabilities for suppression files
- Refactor suppression handling to include start and end dates
- Replace token variables in the reporter template with vulnerability information
- Normalize whitespace in the reasoning field during TaskBuilder construction
- Fix suppression generation to only ignore vulnerabilities that have status fixed
- Handle empty lines in suppression template generation correctly
- Separate Vulnlog definition and vulnerability file reading and parsing in own service class
- Use injected repository instead of return type repositories
- Extract reporting and suppression features into separate modules
- Refactor suppression service to use SubcommandData and improve writer injection
- Update CHANGELOG.md
- Revamp readme file
- Bump Gradle to version 8.14.2
- Add a spinner when run in the console
- Release version 0.9.0

### Fixed

- Fixup reporter association bug

## [0.8.0] - 2025-06-01

### Added

- Add advanced filtering in the HTML report
- Add missing Changelog entries
- Add a search clear button next to the HTML report search field

### Changed

- Start next release
- Fix missing task information in the HTML report table view
- Consolidate logo resources and improve theme handling
- Minify embedded assets in HTML report generation.
- Minify CSS and JS resources to reduce output file size of HTML reports
- Update plugin to support multiple release branches
- Enhance documentation and fix minor typos
- Fix status to affected for permanently suppressed vulnerabilities
- Bump Gradle to version 8.14
- Update HTML report sorting to prioritize _Rating_, _Affected_, and _Fix_ columns in descending order
- HTML report filter button now visually indicates whether a condition is active or not
- Release version 0.8.0

### Removed

- Remove the fix version when a vulnerability is affecting the project but permanently suppressed

## [0.7.1] - 2025-04-24

### Changed

- Start next release
- Remove include requirement of the temporarily specifier in the DSL
- Fix undefined task data when parsing JSON data into a table format
- Make HTML report self-contained by inlining all the external dependencies
- Release version 0.7.1

## [0.7.0] - 2025-04-19

### Added

- Add vulnerability service
- Add affected and fixed release version in report
- Add new data classes for split vulnerabilities for a cleaner separation of multi release branch combined vulnerabilities and single release branch vulnerabilities
- Add a status per vulnerability to indicate in what state a reported vulnerability is.
- Add issue templates for feature requests and bug reports
- Add logo and small re-arrangements in the HTML report

### Changed

- Start next release
- Refactoring
- Refactor Vulnlog report DSL
- Refactor Vulnlog analysis DSL
- Refactor Vulnlog task DSL
- Refactor Vulnlog execution DSL
- Refactor DSL package and remove impl classes
- Move reporter implementation class into separate file
- Various HTML report improvements
- Fix date serialisation
- Use DataTable and Bulma for nicer and more flexible table
- Implement child row details in report
- Fix report child row printing when no task or execution is defined in DSL
- Introduce the fixedAt execution statement
- Extract splitting from filtering
- Support multiple reporter for the same vulnerability #22
- Introduce Koin dependency injection and refactor code base to use DI
- Fix task details string construction by removing the comma between the words
- Fix empty reasoning rendering in the HTML report
- Release version 0.7.0

## [0.6.0] - 2025-03-15

### Added

- Add Mastodon social links to README.md
- Add documented interfaces in DSL package
- Add more DSL API documentation
- Support for more user-friendly release branch names
- Add reporter provider DSL

### Changed

- Start next release
- Fix a duplication in the README.md
- Move DSL implementation classes to dsl-interpreter Gradle project to reduce the DSL package size
- Activate Kotlin explicit API mode in DSL package to prevent unintentional API changes
- Introduce five verdict types and deprecate existing string based verdict
- Introduce default reporter to easily define who found a vulnerability
- Refactor release branch providing
- Release version 0.6.0

### Removed

- Remove impl packages from the Dokka HTML API documentation

## [0.5.3] - 2025-03-12

### Changed

- Fix CI release publishing pipeline
- Release version 0.5.3

## [0.5.2] - 2025-03-12

### Changed

- Start next release
- Fix API documentation output path
- Split CI release publishing pipeline into separate stages
- Release version 0.5.2

## [0.5.1] - 2025-03-12

### Added

- Add caching to increase vulnlog file processing
- Add Kotlin scripting DSL annotation to prevent invalid DSL nesting
- Add CLI download and version printing functionality to the Vulnlog Gradle plugin
- Add support for JSON output on release branch and release versions
- Add serialisable support for ids, report and analysis
- Add serialisable support for tasks
- Add serialisable support for executions
- Add report subcommand to generate a simple HTML report
- Add README.md and CONTRIBUTING.md
- Add report generation task to Gradle plugin
- Add Gradle plugin publishing to CI

### Changed

- Start next release
- Bump Gradle to version 8.12
- Introduce reworked Vulnlog DSL
- Update CHANGELOG.md
- Rename Gradle DSL plugin
- Use the same group in all Gradle projects
- Release version 0.4.0
- Start next release
- Fix Gradle plugin dependency group name
- Separate classes according their purpose
- Split filtering and printing into separate classes
- Enhance DSL filtering by release branch and release version information
- Simplify serialisation by utilising Kotlin extension functions
- Trim JSON structure
- Make CLI and DSL configurable in the Gradle plugin
- Bump Gradle to version 8.13
- Fix release 0.4.0 date in CHANGELOG.md
- Rework execution DSL
- Change DSL filter to filter out release branches without vulnerabilities
- Fix DSL dependency on compile classpath in Gradle plugin
- Update TROUBLESHOOTING.md
- Reduce GitHub action files and simplify CI build
- Fix snapshot releasing
- Update README.md with a caution warning and the project draft logo
- Release version 0.5.0
- Start next release
- Fix release publishing DSL API documentation upload
- Update pull request CI pipeline
- Release version 0.5.1

### Removed

- Remove older DSL versions

## [0.3.3] - 2024-12-21

### Changed

- Start next release
- Rework CI pipeline files
- Upload CLI release artifacts to webserver
- Release version 0.3.3

## [0.3.2] - 2024-12-21

### Changed

- Fix CI pipelines
- Rename CI pipelines
- Set CLI artifact name to 'vl' instead of 'cli'
- Release version 0.3.2

## [0.3.1] - 2024-12-20

### Changed

- Start next release
- Create FUNDING.yml
- Rework GitHub actions pipelines
- Release version 0.3.1

### Removed

- Remove large API documentation files

## [0.3.0] - 2024-12-16

### Added

- Add Gradle build file description and small code refactoring
- Add overwrite mechanic for vulnerabilities
- Add CLI MVP
- Add version flag to CLI application
- Add GitHub action for release automation of the CLI application

### Changed

- Start next release
- Activate Gradle parallel, caching and configuration caching feature
- Use gradle.properties to specify software and DSL version
- Move DSL implementation classes to DSL consuming interpreter package
- Publish DSL API to website
- Update changelog for next release
- Split vulnerability data per branch
- Set correct fixed in date after processing vulnerability data
- Release version 0.3.0

### Removed

- Remove old report and suppression files

## [.0.2.0] - 2024-11-24

### Added

- Add default project setup
- Add PoC DSL
- Add project vulnerability logging to this project
- Add OWASP Dependency Checker suppression file generation PoC
- Add Snyk suppression file generation PoC
- Add GitHub build action
- Add ktlint gradle check
- Add detekt gradle check
- Add OWASP Dependency Checker
- Add demonstraion suppression entries into project vuln log
- Add CI jobs timeouts
- Add troubleshooting guide
- Add GPLv3 license file
- Add lightweight verify CI job
- Add mitigate and remove ignore resolution
- Add a simple HTML PoC report with a basic table of JSON data
- Support multiple rows show details simultaneously
- Add three separate tables for open, known open and resolved vulnerabilities
- Add simple dynamic summary
- Add more realistic dummy data into table
- Add simple dark theme and auto select it when browser supports theming
- Add square emoji summary with theme support
- Add support for id URL query parameter
- Add CSS styling default for box sizing
- Add HTML meta information to improve Google Chrome Lighthouse scan
- Add better mobile view presentation
- Add PoC for branch specific report

### Changed

- Init
- Create dsl project
- Bump Gradle to version 8.6
- Improve DSL, provide executable CLI and add simple unit tests
- Use Gradle version catalog and migrate to convention plugins
- Use Gradle dependency lock files for all projects
- Bump Gradle to version 8.7
- Lint code according to ktlint default ruleset
- Small fixes so the code passes detekt SCA
- Generate OWASP suppression file for CI security job run
- Improve CLI functionality
- Rename generated CLI executable from cli to vl
- Separate logic and introduce dependency injection
- Migrate to improved and simplified DSL
- Bump Gradle to version 8.8
- Fix missing interface bind in dependency injection module
- Clean productive vuln log file
- Uniform package naming to ch.addere.vulnlog
- Introduce java test fixture Gradle plugin and share test helper class
- Rename verify job
- Several small improvements and version updates
- Add snyk open source SCA scanner
- Generate Snyk and OWASP suppression files
- Update ktlint and suppress vulnerabilities
- Allow duplicate Snyk IDs
- Add resolutions without affected version as simplification
- Add publication for the vulnlog script language
- Rename project from ch.addere to io.vulnlog
- Style rating column
- Fix page jump to top on show/hide details click
- Make rating letter colours better readable
- Merge three tables into one single table
- Improve empty cells visualisation
- Clean Javascript code a bit
- New DSL ideas do not push to public
- PoCing a new DSL with reporting in mind
- Simple Gradle Plugin adding DSL to project dependency
- Improve DSL
- Improve DSL by splitting functions into separate interfaces
- DSL rename version to release and split into separate interfaces for published and not yet published releases
- Implement DSL classes
- Improve auto formatting by specifying common max line length
- Bump Gradle to version 8.11
- Bump Kotlin to version 2.0.20
- Bump JDK to version 21
- Implement DSL classes
- Rename DSL packages
- Rename projects package name from io to dev
- Setup manual build publishing
- Release version 0.2.0

### Removed

- Remove properties in vuln context block to simplify the DSL
- Remove all old DSL code

[0.9.1-test1]: https://github.com/vulnlog/vulnlog/compare/v0.9.0..v0.9.1-test1
[0.9.0]: https://github.com/vulnlog/vulnlog/compare/v0.8.0..v0.9.0
[0.8.0]: https://github.com/vulnlog/vulnlog/compare/v0.7.1..v0.8.0
[0.7.1]: https://github.com/vulnlog/vulnlog/compare/v0.7.0..v0.7.1
[0.7.0]: https://github.com/vulnlog/vulnlog/compare/v0.6.0..v0.7.0
[0.6.0]: https://github.com/vulnlog/vulnlog/compare/v0.5.3..v0.6.0
[0.5.3]: https://github.com/vulnlog/vulnlog/compare/v0.5.2..v0.5.3
[0.5.2]: https://github.com/vulnlog/vulnlog/compare/v0.5.1..v0.5.2
[0.5.1]: https://github.com/vulnlog/vulnlog/compare/v0.3.3..v0.5.1
[0.3.3]: https://github.com/vulnlog/vulnlog/compare/v0.3.2..v0.3.3
[0.3.2]: https://github.com/vulnlog/vulnlog/compare/v0.3.1..v0.3.2
[0.3.1]: https://github.com/vulnlog/vulnlog/compare/v0.3.0..v0.3.1
[0.3.0]: https://github.com/vulnlog/vulnlog/compare/v.0.2.0..v0.3.0
[.0.2.0]: https://github.com/vulnlog/vulnlog/tree/v.0.2.0

<!-- generated by git-cliff -->
