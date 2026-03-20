package dev.vulnlog.cli.core

import dev.vulnlog.cli.model.ParseValidationVersion
import dev.vulnlog.cli.model.Project
import dev.vulnlog.cli.model.Release
import dev.vulnlog.cli.model.ReleaseEntry
import dev.vulnlog.cli.model.ReportEntry
import dev.vulnlog.cli.model.ReporterType
import dev.vulnlog.cli.model.SchemaVersion
import dev.vulnlog.cli.model.Tag
import dev.vulnlog.cli.model.TagEntry
import dev.vulnlog.cli.model.VulnId
import dev.vulnlog.cli.model.VulnerabilityEntry
import dev.vulnlog.cli.model.VulnlogFile
import dev.vulnlog.cli.model.VulnlogFileContext
import dev.vulnlog.cli.result.Rule
import dev.vulnlog.cli.result.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDate

private val defaultSchema = SchemaVersion(1, 0)

private fun context(vulnlogFile: VulnlogFile) = VulnlogFileContext(ParseValidationVersion.V1, "dummy.yaml", vulnlogFile)

private fun emptyFile() =
    VulnlogFile(
        schemaVersion = defaultSchema,
        project = Project("org", "project", "author"),
        releases = emptyList(),
        vulnerabilities = emptyList(),
    )

class ValidatorTest : FunSpec({

    test("valid file with no releases or vulnerabilities has no findings") {
        val result = validate(context(emptyFile()))
        result.findings.shouldBeEmpty()
    }

    context("duplicate release IDs") {
        test("unique release IDs produce no findings") {
            val file =
                emptyFile().copy(
                    releases =
                        listOf(
                            ReleaseEntry(Release("v1.0")),
                            ReleaseEntry(Release("v2.0")),
                        ),
                )
            validate(context(file)).errors.shouldBeEmpty()
        }

        test("duplicate release IDs produce an error") {
            val file =
                emptyFile().copy(
                    releases =
                        listOf(
                            ReleaseEntry(Release("v1.0")),
                            ReleaseEntry(Release("v1.0")),
                        ),
                )
            val errors = validate(context(file)).errors
            errors shouldHaveSize 1
            errors[0].rule shouldBe Rule.DUPLICATE_RELEASE_ID
            errors[0].severity shouldBe Severity.ERROR
            errors[0].path shouldBe "releases[v1.0]"
        }
    }

    context("duplicate tag IDs") {
        test("empty tags list produces no findings") {
            val file = emptyFile().copy(tags = emptyList())
            validate(context(file)).errors.shouldBeEmpty()
        }

        test("unique tag IDs produce no findings") {
            val file =
                emptyFile().copy(
                    tags = listOf(TagEntry(Tag("backend")), TagEntry(Tag("frontend"))),
                )
            validate(context(file)).errors.shouldBeEmpty()
        }

        test("duplicate tag IDs produce an error") {
            val file =
                emptyFile().copy(
                    tags = listOf(TagEntry(Tag("backend")), TagEntry(Tag("backend"))),
                )
            val errors = validate(context(file)).errors
            errors shouldHaveSize 1
            errors[0].rule shouldBe Rule.DUPLICATE_TAG_ID
            errors[0].path shouldBe "tags[backend]"
        }
    }

    context("duplicate vulnerability IDs") {
        test("unique vulnerability IDs produce no findings") {
            val file =
                emptyFile().copy(
                    vulnerabilities =
                        listOf(
                            VulnerabilityEntry(VulnId.Cve("CVE-2021-1"), emptyList(), emptyList()),
                            VulnerabilityEntry(VulnId.Cve("CVE-2021-2"), emptyList(), emptyList()),
                        ),
                )
            validate(context(file)).errors.shouldBeEmpty()
        }

        test("duplicate vulnerability IDs produce an error") {
            val vuln = VulnerabilityEntry(VulnId.Cve("CVE-2021-1"), emptyList(), emptyList())
            val file = emptyFile().copy(vulnerabilities = listOf(vuln, vuln))
            val errors = validate(context(file)).errors
            errors shouldHaveSize 1
            errors[0].rule shouldBe Rule.DUPLICATE_VULNERABILITY_ID
            errors[0].path shouldBe "vulnerabilities[CVE-2021-1]"
        }
    }

    context("dangling release references") {
        test("vulnerability referencing defined release produces no findings") {
            val file =
                emptyFile().copy(
                    releases = listOf(ReleaseEntry(Release("v1.0"))),
                    vulnerabilities =
                        listOf(
                            VulnerabilityEntry(VulnId.Cve("CVE-2021-1"), listOf(Release("v1.0")), emptyList()),
                        ),
                )
            validate(context(file)).errors.shouldBeEmpty()
        }

        test("vulnerability referencing undefined release produces an error") {
            val file =
                emptyFile().copy(
                    releases = listOf(ReleaseEntry(Release("v1.0"))),
                    vulnerabilities =
                        listOf(
                            VulnerabilityEntry(VulnId.Cve("CVE-2021-1"), listOf(Release("v2.0")), emptyList()),
                        ),
                )
            val errors = validate(context(file)).errors
            errors shouldHaveSize 1
            errors[0].rule shouldBe Rule.DANGLING_RELEASE_REFERENCE
            errors[0].path shouldBe "vulnerabilities[CVE-2021-1].releases"
            errors[0].message shouldContain "v2.0"
        }

        test("error message lists defined releases") {
            val file =
                emptyFile().copy(
                    releases = listOf(ReleaseEntry(Release("v1.0")), ReleaseEntry(Release("v1.1"))),
                    vulnerabilities =
                        listOf(
                            VulnerabilityEntry(VulnId.Cve("CVE-2021-1"), listOf(Release("v9.9")), emptyList()),
                        ),
                )
            val errors = validate(context(file)).errors
            errors[0].message shouldContain "v1.0"
            errors[0].message shouldContain "v1.1"
        }
    }

    context("analyzed date before earliest report date") {
        test("analyzed date after report date produces no warnings") {
            val file =
                emptyFile().copy(
                    vulnerabilities =
                        listOf(
                            VulnerabilityEntry(
                                id = VulnId.Cve("CVE-2021-1"),
                                releases = emptyList(),
                                reports = listOf(ReportEntry(ReporterType.GRYPE, LocalDate.of(2021, 1, 1))),
                                analyzedAt = LocalDate.of(2021, 6, 1),
                            ),
                        ),
                )
            validate(context(file)).warnings.shouldBeEmpty()
        }

        test("analyzed date equal to report date produces no warnings") {
            val reportDate = LocalDate.of(2021, 1, 1)
            val file =
                emptyFile().copy(
                    vulnerabilities =
                        listOf(
                            VulnerabilityEntry(
                                id = VulnId.Cve("CVE-2021-1"),
                                releases = emptyList(),
                                reports = listOf(ReportEntry(ReporterType.GRYPE, reportDate)),
                                analyzedAt = reportDate,
                            ),
                        ),
                )
            validate(context(file)).warnings.shouldBeEmpty()
        }

        test("analyzed date before earliest report date produces a warning") {
            val file =
                emptyFile().copy(
                    vulnerabilities =
                        listOf(
                            VulnerabilityEntry(
                                id = VulnId.Cve("CVE-2021-1"),
                                releases = emptyList(),
                                reports = listOf(ReportEntry(ReporterType.GRYPE, LocalDate.of(2021, 6, 1))),
                                analyzedAt = LocalDate.of(2021, 1, 1),
                            ),
                        ),
                )
            val warnings = validate(context(file)).warnings
            warnings shouldHaveSize 1
            warnings[0].rule shouldBe Rule.ANALYSED_BEFORE_REPORTED
            warnings[0].severity shouldBe Severity.WARNING
            warnings[0].path shouldBe "vulnerabilities[CVE-2021-1].analyzed_at"
        }

        test("null analyzedAt produces no warnings") {
            val file =
                emptyFile().copy(
                    vulnerabilities =
                        listOf(
                            VulnerabilityEntry(
                                id = VulnId.Cve("CVE-2021-1"),
                                releases = emptyList(),
                                reports = listOf(ReportEntry(ReporterType.GRYPE, LocalDate.of(2021, 1, 1))),
                                analyzedAt = null,
                            ),
                        ),
                )
            validate(context(file)).warnings.shouldBeEmpty()
        }

        test("report with null date is ignored when finding earliest") {
            val file =
                emptyFile().copy(
                    vulnerabilities =
                        listOf(
                            VulnerabilityEntry(
                                id = VulnId.Cve("CVE-2021-1"),
                                releases = emptyList(),
                                reports =
                                    listOf(
                                        ReportEntry(ReporterType.GRYPE, null),
                                        ReportEntry(ReporterType.SNYK, LocalDate.of(2021, 6, 1)),
                                    ),
                                analyzedAt = LocalDate.of(2021, 1, 1),
                            ),
                        ),
                )
            val warnings = validate(context(file)).warnings
            warnings shouldHaveSize 1
        }

        test("all reports have null dates produces no warnings") {
            val file =
                emptyFile().copy(
                    vulnerabilities =
                        listOf(
                            VulnerabilityEntry(
                                id = VulnId.Cve("CVE-2021-1"),
                                releases = emptyList(),
                                reports = listOf(ReportEntry(ReporterType.GRYPE, null)),
                                analyzedAt = LocalDate.of(2021, 1, 1),
                            ),
                        ),
                )
            validate(context(file)).warnings.shouldBeEmpty()
        }
    }
})
