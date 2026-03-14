package dev.vulnlog.cli.parse.v1

import dev.vulnlog.cli.model.Project
import dev.vulnlog.cli.model.ReleaseEntry
import dev.vulnlog.cli.model.SchemaVersion
import dev.vulnlog.cli.model.VulnerabilityEntry
import dev.vulnlog.cli.model.VulnlogFile
import dev.vulnlog.cli.parse.v1.dto.ProjectDto
import dev.vulnlog.cli.parse.v1.dto.ReleaseEntryDto
import dev.vulnlog.cli.parse.v1.dto.VulnerabilityEntryDto
import dev.vulnlog.cli.parse.v1.dto.VulnlogFileV1Dto

object V1Mapper {
    fun fromDomain(file: VulnlogFile): VulnlogFileV1Dto =
        VulnlogFileV1Dto(
            schemaVersion = file.schemaVersion.toDto(),
            project = file.project.toDto(),
            releases = releasesToDto(file.releases),
            vulnerabilities = vulnerabilitiesToDto(file.vulnerabilities),
        )

    private fun SchemaVersion.toDto(): String = if (minor == 0) "$major" else "$major.$minor"

    private fun Project.toDto(): ProjectDto = ProjectDto(organization, project, author)

    private fun releasesToDto(releases: List<ReleaseEntry>): List<ReleaseEntryDto> =
        releases.map { ReleaseEntryDto(id = it.id) }

    private fun vulnerabilitiesToDto(vulnerabilities: List<VulnerabilityEntry>): List<VulnerabilityEntryDto> =
        vulnerabilities.map { VulnerabilityEntryDto(id = it.id) }
}
