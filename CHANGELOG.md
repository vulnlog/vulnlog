# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.13.0] - 2026-05-03

### Added
- Refine HTML report and add more infromation by @ryru in [#126](https://github.com/vulnlog/vulnlog/pull/126)
- Add copy command and Gradle task by @ryru in [#124](https://github.com/vulnlog/vulnlog/pull/124)
- Introduce the dismissed state in reports by @ryru in [#117](https://github.com/vulnlog/vulnlog/pull/117)
- Add STDOUT in report command by @ryru

### Changed
- Share parsing and validation logic between the commands and the Gradle task by @ryru in [#123](https://github.com/vulnlog/vulnlog/pull/123)

### Documentation
- Add documentation updates for version 0.13.0 by @ryru in [#127](https://github.com/vulnlog/vulnlog/pull/127)

### Fixed
- Add relocation for Jackson annotation in Gradle plugin by @ryru in [#115](https://github.com/vulnlog/vulnlog/pull/115)

## [0.12.0] - 2026-04-19

### Added
- Add Gradle plugin by @ryru in [#98](https://github.com/vulnlog/vulnlog/pull/98)
- Run Docker container as default user instead of root by @ryru in [#90](https://github.com/vulnlog/vulnlog/pull/90)
- Add new copy command to clone vulnerability entries by @ryru in [#89](https://github.com/vulnlog/vulnlog/pull/89)
- Add new reporting command with a simple HTML report by @ryru in [#85](https://github.com/vulnlog/vulnlog/pull/85)

### Changed
- Split code into cli and lib by @ryru in [#97](https://github.com/vulnlog/vulnlog/pull/97)
- Improve JSON schema by @ryru in [#91](https://github.com/vulnlog/vulnlog/pull/91)
- Introduce VulnlogFilter and FilterOptions by @ryru in [#84](https://github.com/vulnlog/vulnlog/pull/84)

### Documentation
- Add release 0.12.0 documentation to main branch by @ryru in [#93](https://github.com/vulnlog/vulnlog/pull/93)

### Fixed
- Hyphen reporter flag support by @ryru in [#113](https://github.com/vulnlog/vulnlog/pull/113)
- Fix native binary copy command and do not copy null or empty lists by @ryru in [#92](https://github.com/vulnlog/vulnlog/pull/92)
- Create suppressions for each reporter-specific vulnerability ID by @ryru in [#88](https://github.com/vulnlog/vulnlog/pull/88)
- Add HTML resource config for native binaries by @ryru in [#87](https://github.com/vulnlog/vulnlog/pull/87)
- Auto-suppress risk_acceptable verdicts and exclude resolved by @ryru in [#83](https://github.com/vulnlog/vulnlog/pull/83)

### New Contributors
* @dependabot[bot] made their first contribution in [#107](https://github.com/vulnlog/vulnlog/pull/107)

## [0.11.0] - 2026-04-06

### Added
- Write to STDOUT in suppress command with -o - by @ryru in [#68](https://github.com/vulnlog/vulnlog/pull/68)
- Add support for stdin and stdout in the CLI by @ryru
- Update landing website and online documentation by @ryru in [#67](https://github.com/vulnlog/vulnlog/pull/67)
- Add Docker image for Vulnlog CLI by @ryru in [#66](https://github.com/vulnlog/vulnlog/pull/66)
- Add verdict-based suppression logic by @ryru
- Add support for generic suppression output format by @ryru in [#61](https://github.com/vulnlog/vulnlog/pull/61)
- Add support for Snyk suppression output format by @ryru in [#60](https://github.com/vulnlog/vulnlog/pull/60)
- Add suppress command to generate Trivy suppression files by @ryru in [#59](https://github.com/vulnlog/vulnlog/pull/59)
- Add more data classes, DTOs and validation rules by @ryru in [#56](https://github.com/vulnlog/vulnlog/pull/56)
- Add YAML schema validation and parsing for Vulnlog files by @ryru in [#54](https://github.com/vulnlog/vulnlog/pull/54)
- Add init command to create a new Vulnlog project file by @ryru in [#53](https://github.com/vulnlog/vulnlog/pull/53)

### Changed
- Support filtering by multiple releases in suppression logic by @ryru in [#62](https://github.com/vulnlog/vulnlog/pull/62)
- Extract validation logic into shared function by @ryru
- Extract file parsing logic to shared function by @ryru
- Enable Gradle dependency locking and remove unused `lib-convention` plugin by @ryru in [#55](https://github.com/vulnlog/vulnlog/pull/55)
- Optimize GraalVM for minimal output size and switch to GraalVM 25 by @ryru
- Configure project to support JDK 25 while targeting JVM 17 by @ryru in [#52](https://github.com/vulnlog/vulnlog/pull/52)
- Add ktlint pre-commit hook and update contribution guidelines by @ryru in [#50](https://github.com/vulnlog/vulnlog/pull/50)

### Documentation
- Revamp project documentation and developer workflows for v0.11.0 by @ryru in [#69](https://github.com/vulnlog/vulnlog/pull/69)
- Complete CLI command documentation and improve flag descriptions by @ryru

### Fixed
- Fix single file validation check in suppress command by @ryru in [#65](https://github.com/vulnlog/vulnlog/pull/65)
- Fix missing reflection configuration for DTOs by @ryru in [#58](https://github.com/vulnlog/vulnlog/pull/58)

## [0.10.0] - 2026-03-08

### Added
- Generate native CLI binaries by @ryru in [#48](https://github.com/vulnlog/vulnlog/pull/48)

### Changed
- Prepare project for future CLI version by @ryru in [#46](https://github.com/vulnlog/vulnlog/pull/46)

## [0.9.4] - 2026-02-20

### Documentation
- Add `MAINTAINERS.md` and improve contribution guidelines in `CONTRIBUTING.md` by @ryru in [#42](https://github.com/vulnlog/vulnlog/pull/42)

## [0.9.2] - 2025-10-11

### Documentation
- Improve readme by @ryru in [#38](https://github.com/vulnlog/vulnlog/pull/38)
- Add release instructions in `Releasing.md` by @ryru

### Fixed
- Fix CLI version resolution in Gradle plugin by @ryru in [#37](https://github.com/vulnlog/vulnlog/pull/37)

## [0.9.1] - 2025-10-09

### Changed
- Add comparison links to CHANGELOG.md by @ryru
- Trigger website deployment on changes by @ryru
- Fix deploy-website workflow to set working directory and cache dependency path by @ryru
- Add project website files to mono repo by @ryru in [#36](https://github.com/vulnlog/vulnlog/pull/36)
- Add Github action workflow to deploy project website to Github pages by @ryru
- Move project files into new directory to create a mono repo by @ryru
- Bump Gradle to version 9.1.0 by @ryru in [#35](https://github.com/vulnlog/vulnlog/pull/35)

### New Contributors
* @github-actions[bot] made their first contribution

## [0.9.0] - 2025-06-29

### Changed
- Add a spinner when run in the console by @ryru
- Bump Gradle to version 8.14.2 by @ryru
- Revamp readme file by @ryru in [#33](https://github.com/vulnlog/vulnlog/pull/33)
- Update CHANGELOG.md by @ryru
- Refactor suppression service to use SubcommandData and improve writer injection by @ryru in [#32](https://github.com/vulnlog/vulnlog/pull/32)
- Extract reporting and suppression features into separate modules by @ryru
- Use injected repository instead of return type repositories by @ryru
- Add branch service to abstract the branch repository and apply branch filtering by @ryru
- Separate Vulnlog definition and vulnerability file reading and parsing in own service class by @ryru
- Add help text for the suppress and report command by @ryru
- Add a suppression task and options to the Gradle plugin by @ryru
- Handle empty lines in suppression template generation correctly by @ryru
- Fix suppression generation to only ignore vulnerabilities that have status fixed by @ryru
- Add support to write generated suppression files to STDOUT or to a file by @ryru
- Normalize whitespace in the reasoning field during TaskBuilder construction by @ryru
- Replace token variables in the reporter template with vulnerability information by @ryru
- Refactor suppression handling to include start and end dates by @ryru
- Filter and collect relevant vulnerabilities for suppression files by @ryru
- Fixup refactor suppression logic to use by @ryru
- Filter vulnerabilities by reporter ID matcher by @ryru
- Refactor to rename and restructure vulnerability records by @ryru
- Refactor suppression logic to use VulnPerBranchAndRecord by @ryru
- Add suppression module and refactor common vulnerability data by @ryru
- Add reporter suppression DSL configuration by @ryru
- Refactor DSL interfaces and implementation for consistency by @ryru

### Fixed
- Fixup reporter association bug by @ryru

## [0.8.0] - 2025-06-01

### Changed
- Add a search clear button next to the HTML report search field by @ryru in [#28](https://github.com/vulnlog/vulnlog/pull/28)
- HTML report filter button now visually indicates whether a condition is active or not by @ryru
- Update HTML report sorting to prioritize _Rating_, _Affected_, and _Fix_ columns in descending order by @ryru
- Bump Gradle to version 8.14 by @ryru
- Remove the fix version when a vulnerability is affecting the project but permanently suppressed by @ryru
- Fix status to affected for permanently suppressed vulnerabilities by @ryru
- Add missing Changelog entries by @ryru
- Enhance documentation and fix minor typos by @ryru in [#25](https://github.com/vulnlog/vulnlog/pull/25)
- Add advanced filtering in the HTML report by @ryru
- Update plugin to support multiple release branches by @ryru
- Minify CSS and JS resources to reduce output file size of HTML reports by @ryru
- Minify embedded assets in HTML report generation. by @ryru
- Consolidate logo resources and improve theme handling by @ryru
- Fix missing task information in the HTML report table view by @ryru

## [0.7.1] - 2025-04-24

### Changed
- Make HTML report self-contained by inlining all the external dependencies by @ryru
- Fix undefined task data when parsing JSON data into a table format by @ryru
- Remove include requirement of the temporarily specifier in the DSL by @ryru

## [0.7.0] - 2025-04-19

### Changed
- Fix empty reasoning rendering in the HTML report by @ryru
- Add logo and small re-arrangements in the HTML report by @ryru
- Fix task details string construction by removing the comma between the words by @ryru
- Introduce Koin dependency injection and refactor code base to use DI by @ryru in [#23](https://github.com/vulnlog/vulnlog/pull/23)
- Support multiple reporter for the same vulnerability #22 by @ryru
- Add issue templates for feature requests and bug reports by @ryru
- Add a status per vulnerability to indicate in what state a reported vulnerability is. by @ryru in [#20](https://github.com/vulnlog/vulnlog/pull/20)
- Add new data classes for split vulnerabilities for a cleaner separation of multi release branch combined vulnerabilities and single release branch vulnerabilities by @ryru
- Extract splitting from filtering by @ryru
- Introduce the fixedAt execution statement by @ryru
- Fix report child row printing when no task or execution is defined in DSL by @ryru
- Implement child row details in report by @ryru in [#14](https://github.com/vulnlog/vulnlog/pull/14)
- Use DataTable and Bulma for nicer and more flexible table by @ryru
- Fix date serialisation by @ryru
- Various HTML report improvements by @ryru
- Add affected and fixed release version in report by @ryru in [#13](https://github.com/vulnlog/vulnlog/pull/13)
- Add vulnerability service by @ryru
- Move reporter implementation class into separate file by @ryru
- Refactor DSL package and remove impl classes by @ryru
- Refactor Vulnlog execution DSL by @ryru
- Refactor Vulnlog task DSL by @ryru
- Refactor Vulnlog analysis DSL by @ryru
- Refactor Vulnlog report DSL by @ryru
- Refactoring by @ryru

## [0.6.0] - 2025-03-15

### Changed
- Add reporter provider DSL by @ryru in [#12](https://github.com/vulnlog/vulnlog/pull/12)
- Support for more user-friendly release branch names by @ryru
- Refactor release branch providing by @ryru
- Introduce default reporter to easily define who found a vulnerability by @ryru
- Introduce five verdict types and deprecate existing string based verdict by @ryru in [#11](https://github.com/vulnlog/vulnlog/pull/11)
- Activate Kotlin explicit API mode in DSL package to prevent unintentional API changes by @ryru
- Add more DSL API documentation by @ryru
- Move DSL implementation classes to dsl-interpreter Gradle project to reduce the DSL package size by @ryru
- Add documented interfaces in DSL package by @ryru
- Remove impl packages from the Dokka HTML API documentation by @ryru
- Add Mastodon social links to README.md by @ryru
- Fix a duplication in the README.md by @ryru

## [0.5.3] - 2025-03-12

### Changed
- Fix CI release publishing pipeline by @ryru

## [0.5.2] - 2025-03-12

### Changed
- Split CI release publishing pipeline into separate stages by @ryru
- Fix API documentation output path by @ryru

## [0.5.1] - 2025-03-12

### Changed
- Add Gradle plugin publishing to CI by @ryru in [#10](https://github.com/vulnlog/vulnlog/pull/10)
- Update pull request CI pipeline by @ryru in [#9](https://github.com/vulnlog/vulnlog/pull/9)
- Fix release publishing DSL API documentation upload by @ryru
- Update README.md with a caution warning and the project draft logo by @ryru
- Add report generation task to Gradle plugin by @ryru
- Fix snapshot releasing by @ryru
- Reduce GitHub action files and simplify CI build by @ryru in [#7](https://github.com/vulnlog/vulnlog/pull/7)
- Add README.md and CONTRIBUTING.md by @ryru
- Update TROUBLESHOOTING.md by @ryru
- Fix DSL dependency on compile classpath in Gradle plugin by @ryru
- Add report subcommand to generate a simple HTML report by @ryru
- Change DSL filter to filter out release branches without vulnerabilities by @ryru
- Rework execution DSL by @ryru
- Fix release 0.4.0 date in CHANGELOG.md by @ryru
- Bump Gradle to version 8.13 by @ryru
- Make CLI and DSL configurable in the Gradle plugin by @ryru
- Add serialisable support for executions by @ryru
- Add serialisable support for tasks by @ryru
- Add serialisable support for ids, report and analysis by @ryru
- Trim JSON structure by @ryru
- Simplify serialisation by utilising Kotlin extension functions by @ryru
- Add support for JSON output on release branch and release versions by @ryru
- Enhance DSL filtering by release branch and release version information by @ryru
- Split filtering and printing into separate classes by @ryru
- Add CLI download and version printing functionality to the Vulnlog Gradle plugin by @ryru
- Add Kotlin scripting DSL annotation to prevent invalid DSL nesting by @ryru
- Separate classes according their purpose by @ryru
- Fix Gradle plugin dependency group name by @ryru
- Use the same group in all Gradle projects by @ryru
- Rename Gradle DSL plugin by @ryru
- Update CHANGELOG.md by @ryru
- Remove older DSL versions by @ryru
- Introduce reworked Vulnlog DSL by @ryru
- Bump Gradle to version 8.12 by @ryru
- Add caching to increase vulnlog file processing by @ryru

## [0.3.3] - 2024-12-21

### Changed
- Upload CLI release artifacts to webserver by @ryru
- Rework CI pipeline files by @ryru

## [0.3.2] - 2024-12-21

### Changed
- Set CLI artifact name to 'vl' instead of 'cli' by @ryru
- Rename CI pipelines by @ryru
- Fix CI pipelines by @ryru

## [0.3.1] - 2024-12-20

### Changed
- Remove large API documentation files by @ryru
- Rework GitHub actions pipelines by @ryru
- Create FUNDING.yml by @ryru

## [0.3.0] - 2024-12-16

### Changed
- Add GitHub action for release automation of the CLI application by @ryru
- Add version flag to CLI application by @ryru
- Add CLI MVP by @ryru
- Add overwrite mechanic for vulnerabilities by @ryru
- Set correct fixed in date after processing vulnerability data by @ryru
- Split vulnerability data per branch by @ryru
- Update changelog for next release by @ryru
- Publish DSL API to website by @ryru
- Move DSL implementation classes to DSL consuming interpreter package by @ryru
- Add Gradle build file description and small code refactoring by @ryru
- Remove old report and suppression files by @ryru
- Use gradle.properties to specify software and DSL version by @ryru
- Activate Gradle parallel, caching and configuration caching feature by @ryru

## [.0.2.0] - 2024-11-24

### Changed
- Setup manual build publishing by @ryru
- Rename projects package name from io to dev by @ryru
- Rename DSL packages by @ryru
- Implement DSL classes by @ryru
- Remove all old DSL code by @ryru
- Remove properties in vuln context block to simplify the DSL by @ryru
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
- Add PoC for branch specific report by @ryru
- Add better mobile view presentation by @ryru
- Clean Javascript code a bit by @ryru
- Add HTML meta information to improve Google Chrome Lighthouse scan by @ryru
- Add CSS styling default for box sizing by @ryru
- Improve empty cells visualisation by @ryru
- Merge three tables into one single table by @ryru
- Make rating letter colours better readable by @ryru
- Add support for id URL query parameter by @ryru
- Add square emoji summary with theme support by @ryru
- Fix page jump to top on show/hide details click by @ryru
- Add simple dark theme and auto select it when browser supports theming by @ryru
- Style rating column by @ryru
- Add more realistic dummy data into table by @ryru
- Add simple dynamic summary by @ryru
- Add three separate tables for open, known open and resolved vulnerabilities by @ryru
- Support multiple rows show details simultaneously by @ryru
- Add a simple HTML PoC report with a basic table of JSON data by @ryru
- Rename project from ch.addere to io.vulnlog by @ryru in [#2](https://github.com/vulnlog/vulnlog/pull/2)
- Add publication for the vulnlog script language by @ryru
- Add resolutions without affected version as simplification by @ryru
- Allow duplicate Snyk IDs by @ryru
- Update ktlint and suppress vulnerabilities by @ryru
- Generate Snyk and OWASP suppression files by @ryru
- Add snyk open source SCA scanner by @ryru
- Several small improvements and version updates by @ryru
- Rename verify job by @ryru
- Add mitigate and remove ignore resolution by @ryru
- Add lightweight verify CI job by @ryru
- Add GPLv3 license file by @ryru
- Introduce java test fixture Gradle plugin and share test helper class by @ryru
- Uniform package naming to ch.addere.vulnlog by @ryru
- Clean productive vuln log file by @ryru
- Fix missing interface bind in dependency injection module by @ryru
- Bump Gradle to version 8.8 by @ryru
- Add troubleshooting guide by @ryru
- Migrate to improved and simplified DSL by @ryru
- Separate logic and introduce dependency injection by @ryru
- Add CI jobs timeouts by @ryru
- Rename generated CLI executable from cli to vl by @ryru
- Improve CLI functionality by @ryru
- Add demonstraion suppression entries into project vuln log by @ryru
- Generate OWASP suppression file for CI security job run by @ryru
- Add OWASP Dependency Checker by @ryru
- Add detekt gradle check by @ryru
- Small fixes so the code passes detekt SCA by @ryru
- Add ktlint gradle check by @ryru
- Lint code according to ktlint default ruleset by @ryru
- Add GitHub build action by @ryru
- Bump Gradle to version 8.7 by @ryru
- Use Gradle dependency lock files for all projects by @ryru
- Use Gradle version catalog and migrate to convention plugins by @ryru
- Add Snyk suppression file generation PoC by @ryru
- Add OWASP Dependency Checker suppression file generation PoC by @ryru
- Add project vulnerability logging to this project by @ryru
- Improve DSL, provide executable CLI and add simple unit tests by @ryru
- Bump Gradle to version 8.6 by @ryru
- Add PoC DSL by @ryru
- Create dsl project by @ryru
- Add default project setup by @ryru
- Init by @ryru

[0.13.0]: https://github.com/vulnlog/vulnlog/compare/v0.12.0...v0.13.0
[0.12.0]: https://github.com/vulnlog/vulnlog/compare/v0.11.0...v0.12.0
[0.11.0]: https://github.com/vulnlog/vulnlog/compare/v0.10.0...v0.11.0
[0.10.0]: https://github.com/vulnlog/vulnlog/compare/v0.9.4...v0.10.0
[0.9.4]: https://github.com/vulnlog/vulnlog/compare/v0.9.3...v0.9.4
[0.9.2]: https://github.com/vulnlog/vulnlog/compare/v0.9.1...v0.9.2
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
