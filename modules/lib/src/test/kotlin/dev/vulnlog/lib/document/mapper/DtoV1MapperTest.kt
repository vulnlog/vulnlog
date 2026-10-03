// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.mapper

import dev.vulnlog.lib.document.DomainMappingResult
import dev.vulnlog.lib.document.dto.ProjectDto
import dev.vulnlog.lib.document.dto.ReleaseEntryDto
import dev.vulnlog.lib.document.dto.ReleasePurlEntryDto
import dev.vulnlog.lib.document.dto.ReportEntryDto
import dev.vulnlog.lib.document.dto.ResolutionDto
import dev.vulnlog.lib.document.dto.SuppressionDto
import dev.vulnlog.lib.document.dto.TagEntryDto
import dev.vulnlog.lib.document.dto.VulnerabilityEntryDto
import dev.vulnlog.lib.document.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.v1Dto
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnerabilityDto
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Suppression
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.TagEntry
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnlogFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDate

private fun toDomain(dto: VulnlogFileV1Dto): VulnlogFile =
    DtoV1Mapper
        .toDomain(SchemaVersion.V1, dto)
        .shouldBeInstanceOf<DomainMappingResult.Mapped>()
        .vulnlogProjectFile

class DtoV1MapperTest :
    FunSpec({

        context("toDto") {
            test("writes the schema version as major only, and the project and releases as they are") {
                val file =
                    vulnlogFile(
                        project = Project("acme", "widget", "alice"),
                        releases = listOf(releaseEntry("v1.0"), releaseEntry("v2.0")),
                    )

                val dto = DtoV1Mapper.toDto(file)

                dto.schemaVersion shouldBe "1"
                dto.project shouldBe ProjectDto("acme", "widget", "alice")
                dto.releases shouldBe listOf(ReleaseEntryDto("v1.0"), ReleaseEntryDto("v2.0"))
            }

            test("writes an untriaged vulnerability without verdict keys") {
                val file = vulnlogFile(vulnerabilities = listOf(vulnerability(cve("CVE-2024-1234"))))

                val dto = DtoV1Mapper.toDto(file)

                dto.vulnerabilities shouldBe listOf(vulnerabilityDto("CVE-2024-1234"))
            }

            test("writes an affected verdict with its severity and its disposition token, if any") {
                val dispositions = listOf(Disposition.WONT_FIX, Disposition.WILL_FIX, null)

                val dtos =
                    dispositions.map { disposition ->
                        DtoV1Mapper.vulnerabilityToDto(
                            vulnerability(cve("CVE-2024-1234"), verdict = Verdict.Affected(Severity.HIGH, disposition)),
                        )
                    }

                dtos.map { Triple(it.verdict, it.severity, it.disposition) } shouldBe
                    listOf(
                        Triple("affected", "high", "wont fix"),
                        Triple("affected", "high", "will fix"),
                        Triple("affected", "high", null),
                    )
            }

            test("carries a report's vulnerability ids and suppression") {
                val report =
                    ReportEntry(
                        reporter = ReporterType.SNYK,
                        vulnIds = setOf(VulnId.Snyk("SNYK-JAVA-FOO-1")),
                        suppress = Suppression(expiresAt = LocalDate.of(2026, 12, 31)),
                    )

                val dto = DtoV1Mapper.vulnerabilityToDto(vulnerability(cve("CVE-2024-1234"), reports = listOf(report)))

                val reportDto = dto.reports.single()
                reportDto.vulnIds shouldBe setOf("SNYK-JAVA-FOO-1")
                reportDto.suppress shouldBe SuppressionDto(expiresAt = LocalDate.of(2026, 12, 31))
            }
        }

        context("toDomain") {
            test("maps the project with its contact, the tags and the releases") {
                val dto =
                    v1Dto(
                        project = ProjectDto("acme", "widget", "alice", "alice@example.com"),
                        tags = listOf(TagEntryDto("backend", "Backend services")),
                        releases = listOf(ReleaseEntryDto("v1.0"), ReleaseEntryDto("v2.0")),
                    )

                val file = toDomain(dto)

                file.project shouldBe Project("acme", "widget", "alice", "alice@example.com")
                file.tags shouldBe listOf(TagEntry(Tag("backend"), "Backend services"))
                file.releases.map { it.id } shouldBe listOf(Release("v1.0"), Release("v2.0"))
            }

            test("maps absent tags, verdict and resolution to their defaults") {
                val dto = v1Dto(tags = null, vulnerabilities = listOf(vulnerabilityDto("CVE-2021-1")))

                val file = toDomain(dto)

                file.tags shouldBe emptyList()
                file.vulnerabilities.single().verdict shouldBe Verdict.UnderInvestigation
                file.vulnerabilities.single().resolution shouldBe null
            }

            test("maps aliases, reports and the resolution") {
                val entry =
                    vulnerabilityDto("CVE-2021-1").copy(
                        aliases = listOf("GHSA-aaaa-bbbb-cccc"),
                        reports = listOf(ReportEntryDto("grype")),
                        resolution = ResolutionDto(release = "v2.0", ref = "https://example.com/fix"),
                    )

                val mapped = toDomain(v1Dto(vulnerabilities = listOf(entry))).vulnerabilities.single()

                mapped.aliases shouldBe listOf(VulnId.Ghsa("GHSA-aaaa-bbbb-cccc"))
                mapped.reports.map { it.reporter } shouldBe listOf(ReporterType.GRYPE)
                mapped.resolution shouldBe resolution("v2.0", ref = "https://example.com/fix")
            }

            test("maps every verdict with its severity, disposition and justification") {
                val entries =
                    listOf(
                        vulnerabilityDto("CVE-2021-1", verdict = "under_investigation"),
                        vulnerabilityDto("CVE-2021-2", verdict = "affected", severity = "critical"),
                        vulnerabilityDto("CVE-2021-3", verdict = "affected", severity = "low")
                            .copy(disposition = "wont fix"),
                        vulnerabilityDto("CVE-2021-4", verdict = "affected", severity = "high")
                            .copy(disposition = "will fix"),
                        vulnerabilityDto("CVE-2021-5", verdict = "not affected")
                            .copy(justification = "vulnerable code not present"),
                    )

                val verdicts = toDomain(v1Dto(vulnerabilities = entries)).vulnerabilities.map { it.verdict }

                verdicts shouldBe
                    listOf(
                        Verdict.UnderInvestigation,
                        Verdict.Affected(Severity.CRITICAL),
                        Verdict.Affected(Severity.LOW, Disposition.WONT_FIX),
                        Verdict.Affected(Severity.HIGH, Disposition.WILL_FIX),
                        Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_PRESENT),
                    )
            }

            test("reads the legacy risk acceptable verdict as affected and wont fix, and writes it back so") {
                val legacy = v1Dto(vulnerabilities = listOf(vulnerabilityDto("CVE-2021-1", "risk acceptable", "low")))

                val file = toDomain(legacy)
                val rewritten = DtoV1Mapper.toDto(file).vulnerabilities.single()

                file.vulnerabilities.single().verdict shouldBe Verdict.Affected(Severity.LOW, Disposition.WONT_FIX)
                Triple(rewritten.verdict, rewritten.severity, rewritten.disposition) shouldBe
                    Triple("affected", "low", "wont fix")
            }

            test("rejects every value without a domain representation in one pass, each at its path") {
                val entries =
                    listOf(
                        vulnerabilityDto("CVE-2021-1", verdict = "affected", severity = "low")
                            .copy(disposition = "maybe-fix"),
                        vulnerabilityDto("CVE-2021-2", verdict = "not affected")
                            .copy(justification = "vulnerable code not present", disposition = "wont fix"),
                        vulnerabilityDto("CVE-2021-3", verdict = "invalid_verdict"),
                    )

                val result = DtoV1Mapper.toDomain(SchemaVersion.V1, v1Dto(vulnerabilities = entries))

                val problems = result.shouldBeInstanceOf<DomainMappingResult.Rejected>().problems
                problems.map { it.path to it.message } shouldBe
                    listOf(
                        "vulnerabilities[CVE-2021-1].disposition" to "Invalid disposition: maybe-fix",
                        "vulnerabilities[CVE-2021-2].disposition" to "Disposition requires verdict 'affected'.",
                        "vulnerabilities[CVE-2021-3].verdict" to "Invalid verdict: invalid_verdict",
                    )
            }
        }

        // Copy, the reports and `init` go through the domain model, so a value it drops is lost (issue #192).
        test("keeps every value of a file through toDomain and toDto") {
            val entries =
                listOf(
                    VulnerabilityEntryDto(
                        id = "CVE-2021-44228",
                        name = "Log4Shell",
                        description = "Remote code execution in log4j-core",
                        aliases = listOf("GHSA-jfh8-c2jp-5v3q"),
                        releases = listOf("v1.0"),
                        packages = listOf("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1"),
                        reports =
                            listOf(
                                ReportEntryDto(
                                    reporter = "snyk",
                                    at = LocalDate.of(2021, 12, 10),
                                    source = "nightly scan",
                                    vulnIds = setOf("SNYK-JAVA-ORGAPACHELOGGINGLOG4J-2314720"),
                                    suppress = SuppressionDto(expiresAt = LocalDate.of(2022, 1, 31)),
                                ),
                            ),
                        tags = listOf("backend"),
                        analysis = "Reachable through the request logger.",
                        analyzedAt = LocalDate.of(2021, 12, 11),
                        verdict = "affected",
                        severity = "critical",
                        disposition = "will fix",
                        resolution =
                            ResolutionDto(
                                release = "v2.0",
                                at = LocalDate.of(2021, 12, 14),
                                ref = "https://example.com/fix",
                                note = "Upgraded log4j-core.",
                            ),
                        comment = "Tracked in SEC-1.",
                    ),
                    vulnerabilityDto("CVE-2021-2", verdict = "not affected")
                        .copy(justification = "vulnerable code not present"),
                )

            val dto =
                v1Dto(
                    project = ProjectDto("acme", "widget", "alice", "alice@example.com"),
                    tags = listOf(TagEntryDto("backend", "Backend services")),
                    releases =
                        listOf(
                            ReleaseEntryDto(
                                id = "v1.0",
                                publishedAt = LocalDate.of(2021, 12, 1),
                                purls = listOf(ReleasePurlEntryDto("pkg:maven/com.acme/widget@1.0", listOf("backend"))),
                            ),
                            ReleaseEntryDto("v2.0"),
                        ),
                    vulnerabilities = entries,
                )

            val written = DtoV1Mapper.toDto(toDomain(dto))

            written shouldBe dto
        }
    })
