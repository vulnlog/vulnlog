// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.yaml

import dev.vulnlog.lib.document.dto.VulnerabilityEntryDto
import dev.vulnlog.lib.document.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.document.mapper.DtoV1Mapper
import dev.vulnlog.lib.model.VulnlogFile

object YamlWriter {
    fun write(
        file: VulnlogFile,
        includeSchemaHeader: Boolean = true,
    ): String = renderCanonicalDocument(DtoV1Mapper.toDto(file), includeSchemaHeader)

    fun renderCanonicalDocument(
        dto: VulnlogFileV1Dto,
        includeSchemaHeader: Boolean = true,
    ): String {
        val sections =
            buildList {
                add(CanonicalYaml.renderSection("schemaVersion", dto.schemaVersion).trimEnd())
                add(CanonicalYaml.renderSection("project", dto.project).trimEnd())
                dto.tags?.let { add(CanonicalYaml.renderSection("tags", it).trimEnd()) }
                add(CanonicalYaml.renderSection("releases", dto.releases).trimEnd())
                add(vulnerabilitiesSection(dto.vulnerabilities))
            }
        val header = if (includeSchemaHeader) schemaHeader(dto.schemaVersion) + "\n" else ""
        return header + "---\n" + sections.joinToString("\n\n") + "\n"
    }

    fun schemaHeader(schemaVersion: String): String =
        "# \$schema: https://vulnlog.dev/schema/vulnlog-v${schemaVersion.substringBefore('.')}.json"

    private fun vulnerabilitiesSection(entries: List<VulnerabilityEntryDto>): String =
        if (entries.isEmpty()) {
            CanonicalYaml.renderSection("vulnerabilities", entries).trimEnd()
        } else {
            "vulnerabilities:\n\n" +
                entries.joinToString("\n\n") { CanonicalYaml.renderEntryListItem(it) }
        }
}
