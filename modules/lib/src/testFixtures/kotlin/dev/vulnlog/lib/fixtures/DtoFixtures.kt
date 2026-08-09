// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.fixtures

import dev.vulnlog.lib.parse.dto.ProjectDto
import dev.vulnlog.lib.parse.dto.ReleaseEntryDto
import dev.vulnlog.lib.parse.dto.TagEntryDto
import dev.vulnlog.lib.parse.dto.VulnerabilityEntryDto
import dev.vulnlog.lib.parse.dto.VulnlogFileV1Dto

fun v1Dto(
    vulnerabilities: List<VulnerabilityEntryDto> = emptyList(),
    releases: List<ReleaseEntryDto> = emptyList(),
    tags: List<TagEntryDto>? = null,
    project: ProjectDto = ProjectDto("acme", "widget", "alice"),
): VulnlogFileV1Dto =
    VulnlogFileV1Dto(
        schemaVersion = "1",
        project = project,
        tags = tags,
        releases = releases,
        vulnerabilities = vulnerabilities,
    )

fun vulnerabilityDto(
    id: String,
    verdict: String? = null,
    severity: String? = null,
): VulnerabilityEntryDto =
    VulnerabilityEntryDto(
        id = id,
        releases = emptyList(),
        packages = emptyList(),
        reports = emptyList(),
        verdict = verdict,
        severity = severity,
    )
