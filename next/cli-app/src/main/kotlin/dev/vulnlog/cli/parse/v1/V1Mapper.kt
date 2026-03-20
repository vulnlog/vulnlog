package dev.vulnlog.cli.parse.v1

import dev.vulnlog.cli.core.canonical
import dev.vulnlog.cli.core.parseReporter
import dev.vulnlog.cli.core.parseVulnId
import dev.vulnlog.cli.core.shortenSchemaVersion
import dev.vulnlog.cli.model.ParseValidationVersion
import dev.vulnlog.cli.model.Project
import dev.vulnlog.cli.model.Release
import dev.vulnlog.cli.model.ReleaseEntry
import dev.vulnlog.cli.model.ReportEntry
import dev.vulnlog.cli.model.SchemaVersion
import dev.vulnlog.cli.model.Tag
import dev.vulnlog.cli.model.TagEntry
import dev.vulnlog.cli.model.VulnId
import dev.vulnlog.cli.model.VulnerabilityEntry
import dev.vulnlog.cli.model.VulnlogFile
import dev.vulnlog.cli.parse.v1.dto.ProjectDto
import dev.vulnlog.cli.parse.v1.dto.ReleaseEntryDto
import dev.vulnlog.cli.parse.v1.dto.ReportEntryDto
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

    private fun releasesToDto(releases: List<ReleaseEntry>): List<ReleaseEntryDto> {
        return releases.map {
            ReleaseEntryDto(
                id = it.id.value,
                description = it.description,
                publishedAt = it.publicationDate,
            )
        }
    }

    private fun vulnerabilitiesToDto(vulnerabilities: List<VulnerabilityEntry>): List<VulnerabilityEntryDto> {
        return vulnerabilities.map {
            val vulnId =
                when (it.id) {
                    is VulnId.Cve -> it.id.id
                    is VulnId.Ghsa -> it.id.id
                    is VulnId.Rust -> it.id.id
                    is VulnId.Snyk -> it.id.id
                }
            VulnerabilityEntryDto(
                id = vulnId,
                releases = it.releases.map { release -> release.value },
                reports = reportsToDto(it.reports),
                analyzedAt = it.analyzedAt,
            )
        }
    }

    private fun reportsToDto(reports: List<ReportEntry>): List<ReportEntryDto> {
        return reports.map {
            ReportEntryDto(
                reporter = it.reporter.canonical(),
                at = it.at,
            )
        }
    }

    fun toDomain(
        validationVersion: ParseValidationVersion,
        schemaVersion: SchemaVersion,
        dto: VulnlogFileV1Dto,
    ): ParseResult {
        val errors = mutableListOf<String>()

        val project = dto.project.toDomain()
        val tags = tagsToDomain(dto.tags)
        val releases = releasesToDomain(dto.releases)
        val vulnerabilities =
            try {
                vulnerabilitiesToDomain(dto.vulnerabilities)
            } catch (e: IllegalArgumentException) {
                errors.add("Invalid vulnerability ID: ${e.message}")
                emptyList()
            }

        return if (errors.isEmpty()) {
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
        } else {
            ParseResult.Error(errors)
        }
    }

    private fun ProjectDto.toDomain(): Project = Project(organization, name, author, contact)

    private fun tagsToDomain(tags: List<TagEntryDto>?): List<TagEntry> {
        return tags?.map { tag ->
            TagEntry(
                id = Tag(tag.id),
                description = tag.description,
            )
        } ?: emptyList()
    }

    private fun releasesToDomain(releases: List<ReleaseEntryDto>): List<ReleaseEntry> {
        return releases.map { release ->
            ReleaseEntry(
                id = Release(release.id),
                description = release.description,
                publicationDate = release.publishedAt,
            )
        }
    }

    private fun vulnerabilitiesToDomain(vulnerabilities: List<VulnerabilityEntryDto>): List<VulnerabilityEntry> =
        vulnerabilities.map { vulnerability ->
            VulnerabilityEntry(
                id = parseVulnId(vulnerability.id),
                releases = vulnerabilityReleasesToDomain(vulnerability.releases),
                reports = reportsToDomain(vulnerability.reports),
                analyzedAt = vulnerability.analyzedAt,
            )
        }

    private fun vulnerabilityReleasesToDomain(releases: List<String>): List<Release> = releases.map(::Release)

    private fun reportsToDomain(reports: List<ReportEntryDto>): List<ReportEntry> {
        return reports.map { report ->
            ReportEntry(
                reporter = parseReporter(report.reporter),
                at = report.at,
            )
        }
    }
}
