// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.v1

import com.github.packageurl.MalformedPackageURLException
import com.github.packageurl.PackageURL
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.parsePurl
import dev.vulnlog.lib.core.parseReporter
import dev.vulnlog.lib.core.parseVulnId
import dev.vulnlog.lib.core.shortenSchemaVersion
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.PurlEntry
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.Resolution
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Suppression
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.TagEntry
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.validation.ParseFailure
import dev.vulnlog.lib.parse.v1.dto.ProjectDto
import dev.vulnlog.lib.parse.v1.dto.ReleaseEntryDto
import dev.vulnlog.lib.parse.v1.dto.ReleasePurlEntryDto
import dev.vulnlog.lib.parse.v1.dto.ReportEntryDto
import dev.vulnlog.lib.parse.v1.dto.ResolutionDto
import dev.vulnlog.lib.parse.v1.dto.SuppressionDto
import dev.vulnlog.lib.parse.v1.dto.TagEntryDto
import dev.vulnlog.lib.parse.v1.dto.VulnerabilityEntryDto
import dev.vulnlog.lib.parse.v1.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.result.DomainMappingResult

object V1Mapper {
    fun toDto(file: VulnlogFile): VulnlogFileV1Dto =
        VulnlogFileV1Dto(
            schemaVersion = shortenSchemaVersion(file.schemaVersion),
            project = file.project.toDto(),
            tags = file.tags.map { TagEntryDto(it.id.value, it.description) }.takeIf { it.isNotEmpty() },
            releases = releasesToDto(file.releases),
            vulnerabilities = vulnerabilitiesToDto(file.vulnerabilities),
        )

    private fun Project.toDto(): ProjectDto = ProjectDto(organization, name, author, contact)

    private fun releasesToDto(releases: List<ReleaseEntry>): List<ReleaseEntryDto> =
        releases.map {
            ReleaseEntryDto(
                id = it.id.value,
                publishedAt = it.publicationDate,
            )
        }

    private fun vulnerabilitiesToDto(vulnerabilities: List<VulnerabilityEntry>): List<VulnerabilityEntryDto> =
        vulnerabilities.map(::vulnerabilityToDto)

    fun vulnerabilityToDto(entry: VulnerabilityEntry): VulnerabilityEntryDto {
        val vulnId =
            when (entry.id) {
                is VulnId.Cve -> entry.id.id
                is VulnId.Ghsa -> entry.id.id
                is VulnId.RustSec -> entry.id.id
                is VulnId.Snyk -> entry.id.id
            }
        val aliases =
            entry.aliases.map { alias ->
                when (alias) {
                    is VulnId.Cve -> alias.id
                    is VulnId.Ghsa -> alias.id
                    is VulnId.RustSec -> alias.id
                    is VulnId.Snyk -> alias.id
                }
            }
        return VulnerabilityEntryDto(
            id = vulnId,
            name = entry.name,
            aliases = aliases,
            description = entry.description,
            releases = entry.releases.map { release -> release.value },
            reports = reportsToDto(entry.reports),
            tags = entry.tags.map { tag -> tag.value },
            packages = entry.packages.map { purl -> purl.value },
            analysis = entry.analysis,
            analyzedAt = entry.analyzedAt,
            verdict = verdictToString(entry.verdict),
            severity = severityToString(entry.verdict),
            justification = justificationToString(entry.verdict),
            resolution = entry.resolution?.let { resolutionToDto(it) },
            comment = entry.comment,
        )
    }

    private fun reportsToDto(reports: List<ReportEntry>): List<ReportEntryDto> =
        reports.map {
            ReportEntryDto(
                reporter = it.reporter.canonical(),
                at = it.at,
                source = it.source,
                vulnIds = it.vulnIds.map { vulnId -> vulnId.id }.toSet(),
                suppress = it.suppress?.let { suppression -> SuppressionDto(expiresAt = suppression.expiresAt) },
            )
        }

    /**
     * Maps the DTO onto the domain model, collecting every value without a domain representation
     * (unknown vulnerability id, purl, verdict, ...) together with the entry path it sits at.
     */
    fun toDomain(
        schemaVersion: SchemaVersion,
        dto: VulnlogFileV1Dto,
    ): DomainMappingResult {
        val failures = FailureCollector()
        val file =
            VulnlogFile(
                schemaVersion = schemaVersion,
                project = dto.project.toDomain(),
                tags = tagsToDomain(dto.tags),
                releases = releasesToDomain(dto.releases, failures),
                vulnerabilities = vulnerabilitiesToDomain(dto.vulnerabilities, failures),
            )
        return if (failures.isEmpty()) {
            DomainMappingResult.Valid(file)
        } else {
            DomainMappingResult.Invalid(failures.toList())
        }
    }

    /** Collects mapping failures so a single pass reports every problem (issue #208). */
    private class FailureCollector {
        private val failures = mutableListOf<ParseFailure>()

        fun <T> attempt(
            path: String,
            block: () -> T,
        ): T? =
            try {
                block()
            } catch (e: IllegalArgumentException) {
                report(path, e.message ?: "Invalid value")
                null
            }

        fun report(
            path: String,
            message: String,
        ) {
            failures += ParseFailure(message, path)
        }

        fun isEmpty(): Boolean = failures.isEmpty()

        fun toList(): List<ParseFailure> = failures.toList()
    }

    private fun ProjectDto.toDomain(): Project = Project(organization, name, author, contact)

    private fun tagsToDomain(tags: List<TagEntryDto>?): List<TagEntry> =
        tags?.map { tag ->
            TagEntry(
                id = Tag(tag.id),
                description = tag.description,
            )
        } ?: emptyList()

    private fun releasesToDomain(
        releases: List<ReleaseEntryDto>,
        failures: FailureCollector,
    ): List<ReleaseEntry> =
        releases.map { release ->
            ReleaseEntry(
                id = Release(release.id),
                publicationDate = release.publishedAt,
                purls = purlsToDomain("releases[${release.id}]", release.purls, failures),
            )
        }

    private fun purlsToDomain(
        parentPath: String,
        purls: List<ReleasePurlEntryDto>?,
        failures: FailureCollector,
    ): List<PurlEntry> =
        purls?.mapIndexedNotNull { index, entry ->
            failures
                .attempt("$parentPath.purls[$index].purl") { parsePurl(entry.purl) }
                ?.let { purl -> PurlEntry(purl = purl, tags = entry.tags.map(::Tag)) }
        } ?: emptyList()

    private fun vulnerabilitiesToDomain(
        vulnerabilities: List<VulnerabilityEntryDto>,
        failures: FailureCollector,
    ): List<VulnerabilityEntry> = vulnerabilities.mapNotNull { vulnerabilityToDomain(it, failures) }

    private fun vulnerabilityToDomain(
        dto: VulnerabilityEntryDto,
        failures: FailureCollector,
    ): VulnerabilityEntry? {
        val path = "vulnerabilities[${dto.id}]"
        val id = failures.attempt("$path.id") { parseVulnId(dto.id) }
        val aliases =
            dto.aliases.mapIndexedNotNull { index, alias ->
                failures.attempt("$path.aliases[$index]") { parseVulnId(alias) }
            }
        val packages =
            dto.packages.mapIndexedNotNull { index, purl ->
                failures.attempt("$path.packages[$index]") { parsePurl(purl) }
            }
        val reports = reportsToDomain(path, dto.reports, failures)
        val verdict = verdictToDomain(path, dto, failures)
        if (id == null || verdict == null) return null
        return VulnerabilityEntry(
            id = id,
            aliases = aliases,
            description = dto.description,
            releases = vulnerabilityReleasesToDomain(dto.releases),
            reports = reports,
            tags = dto.tags.map(::Tag),
            packages = packages,
            analysis = dto.analysis,
            analyzedAt = dto.analyzedAt,
            verdict = verdict,
            resolution = resolutionToDomain(dto.resolution),
            comment = dto.comment,
        )
    }

    private fun verdictToDomain(
        path: String,
        dto: VulnerabilityEntryDto,
        failures: FailureCollector,
    ): Verdict? =
        when (dto.verdict) {
            "affected" -> failures.attempt("$path.severity") { Verdict.Affected(parseSeverity(dto.severity)) }
            "not affected" ->
                failures.attempt(
                    "$path.justification",
                ) { Verdict.NotAffected(parseVexJustification(dto.justification)) }

            "risk acceptable" ->
                failures.attempt(
                    "$path.severity",
                ) { Verdict.RiskAcceptable(parseSeverity(dto.severity)) }
            "under_investigation", null -> Verdict.UnderInvestigation
            else -> {
                failures.report("$path.verdict", "Invalid verdict: ${dto.verdict}")
                null
            }
        }

    private fun resolutionToDomain(dto: ResolutionDto?): Resolution? =
        dto?.let {
            Resolution(
                release = Release(it.release),
                at = it.at,
                ref = it.ref,
                note = it.note,
            )
        }

    private fun parsePurl(purlString: String): Purl {
        try {
            val purl = PackageURL(purlString)
            return parsePurl(purl)
        } catch (e: MalformedPackageURLException) {
            throw IllegalArgumentException("Unsupported PURL: $purlString", e)
        }
    }

    private fun parseSeverity(severity: String?): Severity =
        when (severity) {
            "low" -> Severity.LOW
            "medium" -> Severity.MEDIUM
            "high" -> Severity.HIGH
            "critical" -> Severity.CRITICAL
            else -> throw IllegalArgumentException("Invalid severity: $severity")
        }

    private fun parseVexJustification(justification: String?): VexJustification =
        when (justification) {
            "component not present" -> VexJustification.COMPONENT_NOT_PRESENT
            "inline mitigations already exist" -> VexJustification.INLINE_MITIGATIONS_ALREADY_EXIST
            "vulnerable code cannot be controlled by adversary" ->
                VexJustification.VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY

            "vulnerable code not in execute path" -> VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH
            "vulnerable code not present" -> VexJustification.VULNERABLE_CODE_NOT_PRESENT
            else -> throw IllegalArgumentException("Invalid justification: $justification")
        }

    private fun verdictToString(verdict: Verdict): String? =
        when (verdict) {
            is Verdict.Affected -> "affected"
            is Verdict.NotAffected -> "not affected"
            is Verdict.RiskAcceptable -> "risk acceptable"
            is Verdict.UnderInvestigation -> null
        }

    private fun severityToString(verdict: Verdict): String? =
        when (verdict) {
            is Verdict.Affected -> verdict.severity.name.lowercase()
            is Verdict.RiskAcceptable -> verdict.severity.name.lowercase()
            else -> null
        }

    private fun justificationToString(verdict: Verdict): String? =
        when (verdict) {
            is Verdict.NotAffected ->
                when (verdict.justification) {
                    VexJustification.COMPONENT_NOT_PRESENT -> "component not present"
                    VexJustification.INLINE_MITIGATIONS_ALREADY_EXIST -> "inline mitigations already exist"
                    VexJustification.VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY ->
                        "vulnerable code cannot be controlled by adversary"

                    VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH -> "vulnerable code not in execute path"
                    VexJustification.VULNERABLE_CODE_NOT_PRESENT -> "vulnerable code not present"
                }

            else -> null
        }

    private fun resolutionToDto(resolution: Resolution): ResolutionDto =
        ResolutionDto(
            release = resolution.release.value,
            at = resolution.at,
            ref = resolution.ref,
            note = resolution.note,
        )

    private fun vulnerabilityReleasesToDomain(releases: List<String>): List<Release> = releases.map(::Release)

    private fun reportsToDomain(
        parentPath: String,
        reports: List<ReportEntryDto>,
        failures: FailureCollector,
    ): List<ReportEntry> =
        reports.mapIndexedNotNull { index, report ->
            val path = "$parentPath.reports[$index]"
            val reporter =
                failures.attempt("$path.reporter") { parseReporter(report.reporter) }
                    ?: return@mapIndexedNotNull null
            ReportEntry(
                reporter = reporter,
                at = report.at,
                source = report.source,
                vulnIds =
                    report.vulnIds
                        .mapIndexedNotNull {
                            i,
                            vulnId,
                            ->
                            failures.attempt("$path.vuln_ids[$i]") { parseVulnId(vulnId) }
                        }.toSet(),
                suppress = report.suppress?.let(::suppressionsToDomain),
            )
        }

    private fun suppressionsToDomain(dto: SuppressionDto): Suppression =
        Suppression(
            expiresAt = dto.expiresAt,
        )
}
