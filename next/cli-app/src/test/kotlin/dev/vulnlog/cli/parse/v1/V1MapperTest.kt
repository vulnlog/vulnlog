package dev.vulnlog.cli.parse.v1

import dev.vulnlog.cli.model.ParseValidationVersion
import dev.vulnlog.cli.model.Project
import dev.vulnlog.cli.model.Release
import dev.vulnlog.cli.model.ReleaseEntry
import dev.vulnlog.cli.model.SchemaVersion
import dev.vulnlog.cli.model.Severity
import dev.vulnlog.cli.model.Tag
import dev.vulnlog.cli.model.TagEntry
import dev.vulnlog.cli.model.Verdict
import dev.vulnlog.cli.model.VexJustification
import dev.vulnlog.cli.model.VulnId
import dev.vulnlog.cli.model.VulnerabilityEntry
import dev.vulnlog.cli.model.VulnlogFile
import dev.vulnlog.cli.parse.v1.dto.ProjectDto
import dev.vulnlog.cli.parse.v1.dto.ReleaseEntryDto
import dev.vulnlog.cli.parse.v1.dto.ReportEntryDto
import dev.vulnlog.cli.parse.v1.dto.ResolutionDto
import dev.vulnlog.cli.parse.v1.dto.TagEntryDto
import dev.vulnlog.cli.parse.v1.dto.VulnerabilityEntryDto
import dev.vulnlog.cli.parse.v1.dto.VulnlogFileV1Dto
import dev.vulnlog.cli.result.ParseResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private val defaultSchemaVersion = SchemaVersion(1, 0)
private val defaultProject = ProjectDto("acme", "widget", "alice")

private fun minimalDto(
    vulnerabilities: List<VulnerabilityEntryDto> = emptyList(),
    releases: List<ReleaseEntryDto> = emptyList(),
    tags: List<TagEntryDto>? = null,
) = VulnlogFileV1Dto(
    schemaVersion = "1",
    project = defaultProject,
    releases = releases,
    vulnerabilities = vulnerabilities,
    tags = tags,
)

private fun toDomain(dto: VulnlogFileV1Dto) = V1Mapper.toDomain(ParseValidationVersion.V1, defaultSchemaVersion, dto)

private fun vulnlogFile(
    schemaVersion: SchemaVersion = SchemaVersion(1, 0),
    organization: String = "acme",
    project: String = "widget",
    author: String = "alice",
    releases: List<ReleaseEntry> = emptyList(),
    vulnerabilities: List<VulnerabilityEntry> = emptyList(),
) = VulnlogFile(
    schemaVersion = schemaVersion,
    project = Project(organization, project, author),
    releases = releases,
    vulnerabilities = vulnerabilities,
)

class V1MapperTest :
    FunSpec({

        test("schema version with zero minor is formatted as major only") {
            val dto = V1Mapper.toDto(vulnlogFile(schemaVersion = SchemaVersion(1, 0)))

            dto.schemaVersion shouldBe "1"
        }

        test("schema version with non-zero minor is formatted as major.minor") {
            val dto = V1Mapper.toDto(vulnlogFile(schemaVersion = SchemaVersion(1, 2)))

            dto.schemaVersion shouldBe "1.2"
        }

        test("project fields are mapped to dto") {
            val dto = V1Mapper.toDto(vulnlogFile(organization = "acme", project = "widget", author = "alice"))

            dto.project shouldBe ProjectDto("acme", "widget", "alice")
        }

        test("releases are mapped to dto") {
            val dto =
                V1Mapper.toDto(
                    vulnlogFile(releases = listOf(ReleaseEntry(Release("v1.0")), ReleaseEntry(Release("v2.0")))),
                )

            dto.releases shouldBe listOf(ReleaseEntryDto("v1.0"), ReleaseEntryDto("v2.0"))
        }

        test("vulnerabilities are mapped to dto") {
            val dto =
                V1Mapper.toDto(
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                VulnerabilityEntry(
                                    id = VulnId.Cve("CVE-2024-1234"),
                                    releases = emptyList(),
                                    packages = emptyList(),
                                    reports = emptyList(),
                                    verdict = Verdict.UnderInvestigation,
                                ),
                            ),
                    ),
                )

            dto.vulnerabilities shouldBe
                listOf(
                    VulnerabilityEntryDto(
                        id = "CVE-2024-1234",
                        releases = emptyList(),
                        packages = emptyList(),
                        reports = emptyList(),
                    ),
                )
        }

        context("toDomain — project mapping") {
            test("project fields are mapped from dto") {
                toDomain(minimalDto())
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.project shouldBe Project("acme", "widget", "alice")
            }

            test("project contact field is mapped when present") {
                val dto = minimalDto().copy(project = ProjectDto("acme", "widget", "alice", "alice@example.com"))

                toDomain(dto)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.project.contact shouldBe "alice@example.com"
            }
        }

        context("toDomain — tags mapping") {
            test("null tags list maps to empty list") {
                toDomain(minimalDto(tags = null))
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.tags shouldBe emptyList()
            }

            test("tag entries are mapped with id and description") {
                val dto = minimalDto(tags = listOf(TagEntryDto("backend", "Backend services")))

                val tags = toDomain(dto).shouldBeInstanceOf<ParseResult.Ok>().content.tags
                tags shouldHaveSize 1
                tags[0] shouldBe TagEntry(Tag("backend"), "Backend services")
            }
        }

        context("toDomain — releases mapping") {
            test("releases are mapped by id") {
                val dto = minimalDto(releases = listOf(ReleaseEntryDto("v1.0"), ReleaseEntryDto("v2.0")))

                val releases = toDomain(dto).shouldBeInstanceOf<ParseResult.Ok>().content.releases
                releases shouldHaveSize 2
                releases[0].id shouldBe Release("v1.0")
                releases[1].id shouldBe Release("v2.0")
            }
        }

        context("toDomain — vulnerability verdict mapping") {
            test("null verdict maps to UnderInvestigation") {
                val dto =
                    minimalDto(
                        vulnerabilities =
                            listOf(
                                VulnerabilityEntryDto(
                                    "CVE-2021-1",
                                    releases = emptyList(),
                                    packages = emptyList(),
                                    reports = emptyList(),
                                ),
                            ),
                    )

                toDomain(dto)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.vulnerabilities[0]
                    .verdict shouldBe Verdict.UnderInvestigation
            }

            test("under_investigation verdict maps to UnderInvestigation") {
                val dto =
                    minimalDto(
                        vulnerabilities =
                            listOf(
                                VulnerabilityEntryDto(
                                    "CVE-2021-1",
                                    releases = emptyList(),
                                    packages = emptyList(),
                                    reports = emptyList(),
                                    verdict = "under_investigation",
                                ),
                            ),
                    )

                toDomain(dto)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.vulnerabilities[0]
                    .verdict shouldBe Verdict.UnderInvestigation
            }

            test("affected verdict with severity maps to Affected") {
                val dto =
                    minimalDto(
                        vulnerabilities =
                            listOf(
                                VulnerabilityEntryDto(
                                    "CVE-2021-1",
                                    releases = emptyList(),
                                    packages = emptyList(),
                                    reports = emptyList(),
                                    verdict = "affected",
                                    severity = "critical",
                                ),
                            ),
                    )

                toDomain(dto)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.vulnerabilities[0]
                    .verdict shouldBe Verdict.Affected(Severity.CRITICAL)
            }

            test("risk acceptable verdict with severity maps to RiskAcceptable") {
                val dto =
                    minimalDto(
                        vulnerabilities =
                            listOf(
                                VulnerabilityEntryDto(
                                    "CVE-2021-1",
                                    releases = emptyList(),
                                    packages = emptyList(),
                                    reports = emptyList(),
                                    verdict = "risk acceptable",
                                    severity = "low",
                                ),
                            ),
                    )

                toDomain(dto)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.vulnerabilities[0]
                    .verdict shouldBe Verdict.RiskAcceptable(Severity.LOW)
            }

            test("not affected verdict with justification maps to NotAffected") {
                val dto =
                    minimalDto(
                        vulnerabilities =
                            listOf(
                                VulnerabilityEntryDto(
                                    "CVE-2021-1",
                                    releases = emptyList(),
                                    packages = emptyList(),
                                    reports = emptyList(),
                                    verdict = "not affected",
                                    justification = "vulnerable code not present",
                                ),
                            ),
                    )

                toDomain(dto)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.vulnerabilities[0]
                    .verdict shouldBe
                    Verdict.NotAffected(
                        VexJustification.VULNERABLE_CODE_NOT_PRESENT,
                    )
            }

            test("unknown verdict returns a parse error") {
                val dto =
                    minimalDto(
                        vulnerabilities =
                            listOf(
                                VulnerabilityEntryDto(
                                    "CVE-2021-1",
                                    releases = emptyList(),
                                    packages = emptyList(),
                                    reports = emptyList(),
                                    verdict = "invalid_verdict",
                                ),
                            ),
                    )

                toDomain(dto).shouldBeInstanceOf<ParseResult.Error>()
            }
        }

        context("toDomain — vulnerability aliases mapping") {
            test("aliases are mapped to VulnId instances") {
                val dto =
                    minimalDto(
                        vulnerabilities =
                            listOf(
                                VulnerabilityEntryDto(
                                    "CVE-2021-1",
                                    aliases = listOf("GHSA-aaaa-bbbb-cccc"),
                                    releases = emptyList(),
                                    packages = emptyList(),
                                    reports = emptyList(),
                                ),
                            ),
                    )

                val aliases =
                    toDomain(dto)
                        .shouldBeInstanceOf<ParseResult.Ok>()
                        .content.vulnerabilities[0]
                        .aliases
                aliases shouldHaveSize 1
                aliases[0] shouldBe VulnId.Ghsa("GHSA-aaaa-bbbb-cccc")
            }
        }

        context("toDomain — report mapping") {
            test("report is mapped with reporter type") {
                val dto =
                    minimalDto(
                        vulnerabilities =
                            listOf(
                                VulnerabilityEntryDto(
                                    "CVE-2021-1",
                                    releases = emptyList(),
                                    packages = emptyList(),
                                    reports = listOf(ReportEntryDto("grype")),
                                ),
                            ),
                    )

                val reports =
                    toDomain(dto)
                        .shouldBeInstanceOf<ParseResult.Ok>()
                        .content.vulnerabilities[0]
                        .reports
                reports shouldHaveSize 1
                reports[0].reporter shouldBe dev.vulnlog.cli.model.ReporterType.GRYPE
            }
        }

        context("toDomain — resolution mapping") {
            test("null resolution maps to null") {
                val dto =
                    minimalDto(
                        vulnerabilities =
                            listOf(
                                VulnerabilityEntryDto(
                                    "CVE-2021-1",
                                    releases = emptyList(),
                                    packages = emptyList(),
                                    reports = emptyList(),
                                ),
                            ),
                    )

                toDomain(dto)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.vulnerabilities[0]
                    .resolution shouldBe null
            }

            test("resolution is mapped with release and ref") {
                val dto =
                    minimalDto(
                        vulnerabilities =
                            listOf(
                                VulnerabilityEntryDto(
                                    "CVE-2021-1",
                                    releases = emptyList(),
                                    packages = emptyList(),
                                    reports = emptyList(),
                                    resolution = ResolutionDto(release = "v2.0", ref = "https://example.com/fix"),
                                ),
                            ),
                    )

                val resolution =
                    toDomain(dto)
                        .shouldBeInstanceOf<ParseResult.Ok>()
                        .content.vulnerabilities[0]
                        .resolution
                resolution?.release shouldBe Release("v2.0")
                resolution?.ref shouldBe "https://example.com/fix"
            }
        }
    })
