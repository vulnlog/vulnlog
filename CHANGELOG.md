# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.9.1] - 2025-10-09

### Added
- Add comparison links to CHANGELOG.md by @ryru
- Add project website files to mono repo by @ryru in [#36](https://github.com/vulnlog/vulnlog/pull/36)
- Add Github action workflow to deploy project website to Github pages by @ryru

### Changed
- Fix rebase issues in changelog updates during CD workflow by @ryru
- Allow non-conventional commits in changelog generation by @ryru
- Strip "v" prefix from version in CD workflow by @ryru
- Simplify app version definition in CD workflow by @ryru
- Update for refs/tags/v0.9.1 by @github-actions[bot]
- Specify branch for changelog updates in CD workflow by @ryru
- Remove unused workflows and legacy release configuration by @ryru
- Add Gradle plugin publishing job to CD workflow by @ryru
- Add DSL publishing job to CD workflow by @ryru
- Only use conventional commits in changelog by @ryru
- Add the simplest continuous deployment workflow by @ryru
- Replace PR validation workflow with simplified CI workflow by @ryru
- Update for v0.9.1-test1 by @github-actions[bot]
- Revamp release configuration template and category mapping by @ryru
- Update for v0.9.1-test1 by @github-actions[bot]
- Update release workflow and configuration by @ryru
- Update for v0.9.1-test1 by @github-actions[bot]
- Add `contents: write` permission to release workflow by @ryru
- Use example config for cliff's keep-a-changelog format by @ryru
- Fix working directory for gradle runs by @ryru
- Bump cliff Github action to version 4 by @ryru
- Rework version and releaseing by @ryru
- Trigger website deployment on changes by @ryru
- Use own webserver instead of github pages by @ryru
- Fix deploy-website workflow to set working directory and cache dependency path by @ryru
- Move project files into new directory to create a mono repo by @ryru
- Bump Gradle to version 9.1.0 by @ryru in [#35](https://github.com/vulnlog/vulnlog/pull/35)
- Start next release by @ryru

### New Contributors
* @github-actions[bot] made their first contribution

## [0.9.0] - 2025-06-29

### Added
- Add branch service to abstract the branch repository and apply branch filtering by @ryru
- Add help text for the suppress and report command by @ryru
- Add a suppression task and options to the Gradle plugin by @ryru
- Add support to write generated suppression files to STDOUT or to a file by @ryru
- Add suppression module and refactor common vulnerability data by @ryru
- Add reporter suppression DSL configuration by @ryru

### Changed
- Release version 0.9.0 by @ryru
- Add a spinner when run in the console by @ryru
- Bump Gradle to version 8.14.2 by @ryru
- Revamp readme file by @ryru in [#33](https://github.com/vulnlog/vulnlog/pull/33)
- Update CHANGELOG.md by @ryru
- Refactor suppression service to use SubcommandData and improve writer injection by @ryru in [#32](https://github.com/vulnlog/vulnlog/pull/32)
- Extract reporting and suppression features into separate modules by @ryru
- Use injected repository instead of return type repositories by @ryru
- Separate Vulnlog definition and vulnerability file reading and parsing in own service class by @ryru
- Handle empty lines in suppression template generation correctly by @ryru
- Fix suppression generation to only ignore vulnerabilities that have status fixed by @ryru
- Normalize whitespace in the reasoning field during TaskBuilder construction by @ryru
- Replace token variables in the reporter template with vulnerability information by @ryru
- Refactor suppression handling to include start and end dates by @ryru
- Filter and collect relevant vulnerabilities for suppression files by @ryru
- Fixup refactor suppression logic to use by @ryru
- Filter vulnerabilities by reporter ID matcher by @ryru
- Refactor to rename and restructure vulnerability records by @ryru
- Refactor suppression logic to use VulnPerBranchAndRecord by @ryru
- Refactor DSL interfaces and implementation for consistency by @ryru
- Start next release by @ryru

### Fixed
- Fixup reporter association bug by @ryru

## [0.8.0] - 2025-06-01

### Added
- Add a search clear button next to the HTML report search field by @ryru in [#28](https://github.com/vulnlog/vulnlog/pull/28)
- Add missing Changelog entries by @ryru
- Add advanced filtering in the HTML report by @ryru

### Changed
- Release version 0.8.0 by @ryru
- HTML report filter button now visually indicates whether a condition is active or not by @ryru
- Update HTML report sorting to prioritize _Rating_, _Affected_, and _Fix_ columns in descending order by @ryru
- Bump Gradle to version 8.14 by @ryru
- Fix status to affected for permanently suppressed vulnerabilities by @ryru
- Enhance documentation and fix minor typos by @ryru in [#25](https://github.com/vulnlog/vulnlog/pull/25)
- Update plugin to support multiple release branches by @ryru
- Minify CSS and JS resources to reduce output file size of HTML reports by @ryru
- Minify embedded assets in HTML report generation. by @ryru
- Consolidate logo resources and improve theme handling by @ryru
- Fix missing task information in the HTML report table view by @ryru
- Start next release by @ryru

### Removed
- Remove the fix version when a vulnerability is affecting the project but permanently suppressed by @ryru

## [0.7.1] - 2025-04-24

### Changed
- Release version 0.7.1 by @ryru
- Make HTML report self-contained by inlining all the external dependencies by @ryru
- Fix undefined task data when parsing JSON data into a table format by @ryru
- Remove include requirement of the temporarily specifier in the DSL by @ryru
- Start next release by @ryru

## [0.7.0] - 2025-04-19

### Added
- Add logo and small re-arrangements in the HTML report by @ryru
- Add issue templates for feature requests and bug reports by @ryru
- Add a status per vulnerability to indicate in what state a reported vulnerability is. by @ryru in [#20](https://github.com/vulnlog/vulnlog/pull/20)
- Add new data classes for split vulnerabilities for a cleaner separation of multi release branch combined vulnerabilities and single release branch vulnerabilities by @ryru
- Add affected and fixed release version in report by @ryru in [#13](https://github.com/vulnlog/vulnlog/pull/13)
- Add vulnerability service by @ryru

### Changed
- Release version 0.7.0 by @ryru
- Fix empty reasoning rendering in the HTML report by @ryru
- Fix task details string construction by removing the comma between the words by @ryru
- Introduce Koin dependency injection and refactor code base to use DI by @ryru in [#23](https://github.com/vulnlog/vulnlog/pull/23)
- Support multiple reporter for the same vulnerability #22 by @ryru
- Extract splitting from filtering by @ryru
- Introduce the fixedAt execution statement by @ryru
- Fix report child row printing when no task or execution is defined in DSL by @ryru
- Implement child row details in report by @ryru in [#14](https://github.com/vulnlog/vulnlog/pull/14)
- Use DataTable and Bulma for nicer and more flexible table by @ryru
- Fix date serialisation by @ryru
- Various HTML report improvements by @ryru
- Move reporter implementation class into separate file by @ryru
- Refactor DSL package and remove impl classes by @ryru
- Refactor Vulnlog execution DSL by @ryru
- Refactor Vulnlog task DSL by @ryru
- Refactor Vulnlog analysis DSL by @ryru
- Refactor Vulnlog report DSL by @ryru
- Refactoring by @ryru
- Start next release by @ryru

## [0.6.0] - 2025-03-15

### Added
- Add reporter provider DSL by @ryru in [#12](https://github.com/vulnlog/vulnlog/pull/12)
- Support for more user-friendly release branch names by @ryru
- Add more DSL API documentation by @ryru
- Add documented interfaces in DSL package by @ryru
- Add Mastodon social links to README.md by @ryru

### Changed
- Release version 0.6.0 by @ryru
- Refactor release branch providing by @ryru
- Introduce default reporter to easily define who found a vulnerability by @ryru
- Introduce five verdict types and deprecate existing string based verdict by @ryru in [#11](https://github.com/vulnlog/vulnlog/pull/11)
- Activate Kotlin explicit API mode in DSL package to prevent unintentional API changes by @ryru
- Move DSL implementation classes to dsl-interpreter Gradle project to reduce the DSL package size by @ryru
- Fix a duplication in the README.md by @ryru
- Start next release by @ryru

### Removed
- Remove impl packages from the Dokka HTML API documentation by @ryru

## [0.5.3] - 2025-03-12

### Changed
- Release version 0.5.3 by @ryru
- Fix CI release publishing pipeline by @ryru

## [0.5.2] - 2025-03-12

### Changed
- Release version 0.5.2 by @ryru
- Split CI release publishing pipeline into separate stages by @ryru
- Fix API documentation output path by @ryru
- Start next release by @ryru

## [0.5.1] - 2025-03-12

### Added
- Add Gradle plugin publishing to CI by @ryru in [#10](https://github.com/vulnlog/vulnlog/pull/10)
- Add report generation task to Gradle plugin by @ryru
- Add README.md and CONTRIBUTING.md by @ryru
- Add report subcommand to generate a simple HTML report by @ryru
- Add serialisable support for executions by @ryru
- Add serialisable support for tasks by @ryru
- Add serialisable support for ids, report and analysis by @ryru
- Add support for JSON output on release branch and release versions by @ryru
- Add CLI download and version printing functionality to the Vulnlog Gradle plugin by @ryru
- Add Kotlin scripting DSL annotation to prevent invalid DSL nesting by @ryru
- Add caching to increase vulnlog file processing by @ryru

### Changed
- Release version 0.5.1 by @ryru
- Update pull request CI pipeline by @ryru in [#9](https://github.com/vulnlog/vulnlog/pull/9)
- Fix release publishing DSL API documentation upload by @ryru
- Start next release by @ryru
- Release version 0.5.0 by @ryru in [#8](https://github.com/vulnlog/vulnlog/pull/8)
- Update README.md with a caution warning and the project draft logo by @ryru
- Fix snapshot releasing by @ryru
- Reduce GitHub action files and simplify CI build by @ryru in [#7](https://github.com/vulnlog/vulnlog/pull/7)
- Update TROUBLESHOOTING.md by @ryru
- Fix DSL dependency on compile classpath in Gradle plugin by @ryru
- Change DSL filter to filter out release branches without vulnerabilities by @ryru
- Rework execution DSL by @ryru
- Fix release 0.4.0 date in CHANGELOG.md by @ryru
- Bump Gradle to version 8.13 by @ryru
- Make CLI and DSL configurable in the Gradle plugin by @ryru
- Trim JSON structure by @ryru
- Simplify serialisation by utilising Kotlin extension functions by @ryru
- Enhance DSL filtering by release branch and release version information by @ryru
- Split filtering and printing into separate classes by @ryru
- Separate classes according their purpose by @ryru
- Fix Gradle plugin dependency group name by @ryru
- Start next release by @ryru
- Release version 0.4.0 by @ryru in [#6](https://github.com/vulnlog/vulnlog/pull/6)
- Use the same group in all Gradle projects by @ryru
- Rename Gradle DSL plugin by @ryru
- Update CHANGELOG.md by @ryru
- Introduce reworked Vulnlog DSL by @ryru
- Bump Gradle to version 8.12 by @ryru
- Start next release by @ryru

### Removed
- Remove older DSL versions by @ryru

## [0.3.3] - 2024-12-21

### Changed
- Release version 0.3.3 by @ryru
- Upload CLI release artifacts to webserver by @ryru
- Rework CI pipeline files by @ryru
- Start next release by @ryru

## [0.3.2] - 2024-12-21

### Changed
- Release version 0.3.2 by @ryru
- Set CLI artifact name to 'vl' instead of 'cli' by @ryru
- Rename CI pipelines by @ryru
- Fix CI pipelines by @ryru

## [0.3.1] - 2024-12-20

### Changed
- Release version 0.3.1 by @ryru in [#3](https://github.com/vulnlog/vulnlog/pull/3)
- Rework GitHub actions pipelines by @ryru
- Create FUNDING.yml by @ryru
- Start next release by @ryru

### Removed
- Remove large API documentation files by @ryru

## [0.3.0] - 2024-12-16

### Added
- Add GitHub action for release automation of the CLI application by @ryru
- Add version flag to CLI application by @ryru
- Add CLI MVP by @ryru
- Add overwrite mechanic for vulnerabilities by @ryru
- Add Gradle build file description and small code refactoring by @ryru

### Changed
- Release version 0.3.0 by @ryru
- Set correct fixed in date after processing vulnerability data by @ryru
- Split vulnerability data per branch by @ryru
- Update changelog for next release by @ryru
- Publish DSL API to website by @ryru
- Move DSL implementation classes to DSL consuming interpreter package by @ryru
- Use gradle.properties to specify software and DSL version by @ryru
- Activate Gradle parallel, caching and configuration caching feature by @ryru
- Start next release by @ryru

### Removed
- Remove old report and suppression files by @ryru

## [.0.2.0] - 2024-11-24

### Added
- Add PoC for branch specific report by @ryru
- Add better mobile view presentation by @ryru
- Add HTML meta information to improve Google Chrome Lighthouse scan by @ryru
- Add CSS styling default for box sizing by @ryru
- Add support for id URL query parameter by @ryru
- Add square emoji summary with theme support by @ryru
- Add simple dark theme and auto select it when browser supports theming by @ryru
- Add more realistic dummy data into table by @ryru
- Add simple dynamic summary by @ryru
- Add three separate tables for open, known open and resolved vulnerabilities by @ryru
- Support multiple rows show details simultaneously by @ryru
- Add a simple HTML PoC report with a basic table of JSON data by @ryru
- Add mitigate and remove ignore resolution by @ryru
- Add lightweight verify CI job by @ryru
- Add GPLv3 license file by @ryru
- Add troubleshooting guide by @ryru
- Add CI jobs timeouts by @ryru
- Add demonstraion suppression entries into project vuln log by @ryru
- Add OWASP Dependency Checker by @ryru
- Add detekt gradle check by @ryru
- Add ktlint gradle check by @ryru
- Add GitHub build action by @ryru
- Add Snyk suppression file generation PoC by @ryru
- Add OWASP Dependency Checker suppression file generation PoC by @ryru
- Add project vulnerability logging to this project by @ryru
- Add PoC DSL by @ryru
- Add default project setup by @ryru

### Changed
- Release version 0.2.0 by @ryru
- Setup manual build publishing by @ryru
- Rename projects package name from io to dev by @ryru
- Rename DSL packages by @ryru
- Implement DSL classes by @ryru
- Bump JDK to version 21 by @ryru
- Bump Kotlin to version 2.0.20 by @ryru
- Bump Gradle to version 8.11 by @ryru
- Improve auto formatting by specifying common max line length by @ryru
- Implement DSL classes by @ryru
- DSL rename version to release and split into separate interfaces for published and not yet published releases by @ryru
- Improve DSL by splitting functions into separate interfaces by @ryru
- Improve DSL by @ryru
- Simple Gradle Plugin adding DSL to project dependency by @ryru
- PoCing a new DSL with reporting in mind by @ryru
- New DSL ideas do not push to public by @ryru
- Clean Javascript code a bit by @ryru
- Improve empty cells visualisation by @ryru
- Merge three tables into one single table by @ryru
- Make rating letter colours better readable by @ryru
- Fix page jump to top on show/hide details click by @ryru
- Style rating column by @ryru
- Rename project from ch.addere to io.vulnlog by @ryru in [#2](https://github.com/vulnlog/vulnlog/pull/2)
- Add publication for the vulnlog script language by @ryru
- Add resolutions without affected version as simplification by @ryru
- Allow duplicate Snyk IDs by @ryru
- Update ktlint and suppress vulnerabilities by @ryru
- Generate Snyk and OWASP suppression files by @ryru
- Add snyk open source SCA scanner by @ryru
- Several small improvements and version updates by @ryru
- Rename verify job by @ryru
- Introduce java test fixture Gradle plugin and share test helper class by @ryru
- Uniform package naming to ch.addere.vulnlog by @ryru
- Clean productive vuln log file by @ryru
- Fix missing interface bind in dependency injection module by @ryru
- Bump Gradle to version 8.8 by @ryru
- Migrate to improved and simplified DSL by @ryru
- Separate logic and introduce dependency injection by @ryru
- Rename generated CLI executable from cli to vl by @ryru
- Improve CLI functionality by @ryru
- Generate OWASP suppression file for CI security job run by @ryru
- Small fixes so the code passes detekt SCA by @ryru
- Lint code according to ktlint default ruleset by @ryru
- Bump Gradle to version 8.7 by @ryru
- Use Gradle dependency lock files for all projects by @ryru
- Use Gradle version catalog and migrate to convention plugins by @ryru
- Improve DSL, provide executable CLI and add simple unit tests by @ryru
- Bump Gradle to version 8.6 by @ryru
- Create dsl project by @ryru
- Init by @ryru

### Removed
- Remove all old DSL code by @ryru
- Remove properties in vuln context block to simplify the DSL by @ryru

[0.9.1]: https://github.com/vulnlog/vulnlog/compare/v0.9.0...v0.9.1
[0.9.0]: https://github.com/vulnlog/vulnlog/compare/v0.8.0...v0.9.0
[0.8.0]: https://github.com/vulnlog/vulnlog/compare/v0.7.1...v0.8.0
[0.7.1]: https://github.com/vulnlog/vulnlog/compare/v0.7.0...v0.7.1
[0.7.0]: https://github.com/vulnlog/vulnlog/compare/v0.6.0...v0.7.0
[0.6.0]: https://github.com/vulnlog/vulnlog/compare/v0.5.3...v0.6.0
[0.5.3]: https://github.com/vulnlog/vulnlog/compare/v0.5.2...v0.5.3
[0.5.2]: https://github.com/vulnlog/vulnlog/compare/v0.5.1...v0.5.2
[0.5.1]: https://github.com/vulnlog/vulnlog/compare/v0.3.3...v0.5.1
[0.3.3]: https://github.com/vulnlog/vulnlog/compare/v0.3.2...v0.3.3
[0.3.2]: https://github.com/vulnlog/vulnlog/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/vulnlog/vulnlog/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/vulnlog/vulnlog/compare/v.0.2.0...v0.3.0

<!-- generated by git-cliff -->
