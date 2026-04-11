package dev.vulnlog.cli.parse.v1

import com.github.packageurl.MalformedPackageURLException
import com.github.packageurl.PackageURL
import dev.vulnlog.cli.core.canonical
import dev.vulnlog.cli.core.parsePurl
import dev.vulnlog.cli.core.parseReporter
import dev.vulnlog.cli.core.parseVulnId
import dev.vulnlog.cli.core.shortenSchemaVersion
import dev.vulnlog.cli.model.ParseValidationVersion
import dev.vulnlog.cli.model.Project
import dev.vulnlog.cli.model.Purl
import dev.vulnlog.cli.model.PurlEntry
import dev.vulnlog.cli.model.Release
import dev.vulnlog.cli.model.ReleaseEntry
import dev.vulnlog.cli.model.ReportEntry
import dev.vulnlog.cli.model.Resolution
import dev.vulnlog.cli.model.SchemaVersion
import dev.vulnlog.cli.model.Severity
import dev.vulnlog.cli.model.Suppression
import dev.vulnlog.cli.model.Tag
import dev.vulnlog.cli.model.TagEntry
import dev.vulnlog.cli.model.Verdict
import dev.vulnlog.cli.model.VexJustification
import dev.vulnlog.cli.model.VulnId
import dev.vulnlog.cli.model.VulnerabilityEntry
import dev.vulnlog.cli.model.VulnlogFile
import dev.vulnlog.cli.parse.v1.dto.ProjectDto
import dev.vulnlog.cli.parse.v1.dto.ReleaseEntryDto
import dev.vulnlog.cli.parse.v1.dto.ReleasePurlEntryDto
import dev.vulnlog.cli.parse.v1.dto.ReportEntryDto
import dev.vulnlog.cli.parse.v1.dto.ResolutionDto
import dev.vulnlog.cli.parse.v1.dto.SuppressionDto
import dev.vulnlog.cli.parse.v1.dto.TagEntryDto
import dev.vulnlog.cli.parse.v1.dto.VulnerabilityEntryDto
import dev.vulnlog.cli.parse.v1.dto.VulnlogFileV1Dto
import dev.vulnlog.cli.result.ParseResult

object V1Mapper {
    fun toDto(file: VulnlogFile): VulnlogFileV1Dto =
        VulnlogFileV1Dto(
            schemaVersion = shortenSchemaVersion(file.schemaVersion),
            project = file.project.toDto(),
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

    internal fun vulnerabilityToDto(entry: VulnerabilityEntry): VulnerabilityEntryDto {
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
            )
        }

    fun toDomain(
        validationVersion: ParseValidationVersion,
        schemaVersion: SchemaVersion,
        dto: VulnlogFileV1Dto,
    ): ParseResult =
        // TODO collect multiple errors and report all back. Something like this:
        //    val errors = mutableListOf<String>()
        //
        //    val vulnerabilities = dto.vulnerabilities.map { vuln ->
        //        val id = parseVulnId(vuln.id)
        //        if (id == null) errors.add("Unknown vulnerability ID format: '${vuln.id}'")
        //        // ...
        //    }
        //
        //    return if (errors.isEmpty()) ParseResult.Ok(...)
        //    else ParseResult.Error(errors.joinToString("\n"))
        try {
            val project = dto.project.toDomain()
            val tags = tagsToDomain(dto.tags)
            val releases = releasesToDomain(dto.releases)
            val vulnerabilities = vulnerabilitiesToDomain(dto.vulnerabilities)

            ParseResult.Ok(
                validationVersion,
                VulnlogFile(
                    schemaVersion = schemaVersion,
                    project = project,
                    tags = tags,
                    releases = releases,
                    vulnerabilities = vulnerabilities,
                ),
            )
        } catch (e: IllegalArgumentException) {
            ParseResult.Error("Parser error: ${e.message}")
        }

    private fun ProjectDto.toDomain(): Project = Project(organization, name, author, contact)

    private fun tagsToDomain(tags: List<TagEntryDto>?): List<TagEntry> =
        tags?.map { tag ->
            TagEntry(
                id = Tag(tag.id),
                description = tag.description,
            )
        } ?: emptyList()

    private fun releasesToDomain(releases: List<ReleaseEntryDto>): List<ReleaseEntry> =
        releases.map { release ->
            ReleaseEntry(
                id = Release(release.id),
                publicationDate = release.publishedAt,
                purls = purlsToDomain(release.purls),
            )
        }

    private fun purlsToDomain(purls: List<ReleasePurlEntryDto>?): List<PurlEntry> =
        purls?.map { purl ->
            PurlEntry(
                purl = parsePurl(purl.purl),
                tags = purl.tags.map(::Tag),
            )
        } ?: emptyList()

    private fun vulnerabilitiesToDomain(vulnerabilities: List<VulnerabilityEntryDto>): List<VulnerabilityEntry> =
        vulnerabilities.map { vulnerability ->
            VulnerabilityEntry(
                id = parseVulnId(vulnerability.id),
                aliases = vulnerability.aliases.map(::parseVulnId),
                description = vulnerability.description,
                releases = vulnerabilityReleasesToDomain(vulnerability.releases),
                reports = reportsToDomain(vulnerability.reports),
                tags = vulnerability.tags.map(::Tag),
                packages = vulnerability.packages.map { parsePurl(it) },
                analysis = vulnerability.analysis,
                analyzedAt = vulnerability.analyzedAt,
                verdict =
                    when (vulnerability.verdict) {
                        "affected" -> Verdict.Affected(parseSeverity(vulnerability.severity))
                        "not_affected" -> Verdict.NotAffected(parseVexJustification(vulnerability.justification))
                        "risk_acceptable" -> Verdict.RiskAcceptable(parseSeverity(vulnerability.severity))
                        "under_investigation" -> Verdict.UnderInvestigation
                        null -> Verdict.UnderInvestigation
                        else -> throw IllegalArgumentException("Invalid verdict: ${vulnerability.verdict}")
                    },
                resolution =
                    if (vulnerability.resolution != null) {
                        Resolution(
                            release = Release(vulnerability.resolution.release),
                            at = vulnerability.resolution.at,
                            ref = vulnerability.resolution.ref,
                            note = vulnerability.resolution.note,
                        )
                    } else {
                        null
                    },
                comment = vulnerability.comment,
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
            "component_not_present" -> VexJustification.COMPONENT_NOT_PRESENT
            "inline_mitigations_already_exists" -> VexJustification.INLINE_MITIGATIONS_ALREADY_EXIST
            "vulnerable_code_cannot_be_controlled_by_adversary" ->
                VexJustification.VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY

            "vulnerable_code_not_in_execute_path" -> VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH
            "vulnerable_code_not_present" -> VexJustification.VULNERABLE_CODE_NOT_PRESENT
            else -> throw IllegalArgumentException("Invalid justification: $justification")
        }

    private fun verdictToString(verdict: Verdict): String? =
        when (verdict) {
            is Verdict.Affected -> "affected"
            is Verdict.NotAffected -> "not_affected"
            is Verdict.RiskAcceptable -> "risk_acceptable"
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
                    VexJustification.COMPONENT_NOT_PRESENT -> "component_not_present"
                    VexJustification.INLINE_MITIGATIONS_ALREADY_EXIST -> "inline_mitigations_already_exists"
                    VexJustification.VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY ->
                        "vulnerable_code_cannot_be_controlled_by_adversary"
                    VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH -> "vulnerable_code_not_in_execute_path"
                    VexJustification.VULNERABLE_CODE_NOT_PRESENT -> "vulnerable_code_not_present"
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

    private fun reportsToDomain(reports: List<ReportEntryDto>): List<ReportEntry> =
        reports.map { report ->
            ReportEntry(
                reporter = parseReporter(report.reporter),
                at = report.at,
                source = report.source,
                vulnIds = report.vulnIds.map(::parseVulnId).toSet(),
                suppress = report.suppress?.let(::suppressionsToDomain),
            )
        }

    private fun suppressionsToDomain(dto: SuppressionDto): Suppression =
        Suppression(
            expiresAt = dto.expiresAt,
        )
}
