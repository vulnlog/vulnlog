// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.parse.CanonicalYaml
import dev.vulnlog.lib.parse.v1.dto.VulnlogFileV1Dto
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

/**
 * Reformats a parsed, validated schema-v1 document to the canonical style ([CanonicalYaml]), splicing each
 * re-rendered section (`schemaVersion`, `project`, `tags`, `releases`) and vulnerability entry back in place.
 * The `# $schema:` header, blank-line layout and inter-block comments survive; comments *inside* a block do not.
 *
 * Rendering round-trips through [VulnlogFileV1Dto], a 1:1 image of the YAML, so no field is dropped.
 */
fun formatYaml(
    rawContent: VulnlogFileRaw,
    mapper: ObjectMapper,
): VulnlogFileRaw {
    val dto = mapper.readValue<VulnlogFileV1Dto>(rawContent.content)
    return VulnlogFileRaw(formatContent(rawContent.content, dto, mapper))
}

sealed interface FormatOutcome {
    data object Unchanged : FormatOutcome

    data class Reformatted(
        val formatted: VulnlogFileRaw,
    ) : FormatOutcome
}

fun formatYamlOutcome(
    rawContent: VulnlogFileRaw,
    mapper: ObjectMapper,
): FormatOutcome {
    val formatted = formatYaml(rawContent, mapper)
    return if (formatted == rawContent) FormatOutcome.Unchanged else FormatOutcome.Reformatted(formatted)
}

private fun formatContent(
    rawContent: String,
    dto: VulnlogFileV1Dto,
    mapper: ObjectMapper,
): String {
    var content = rawContent
    content =
        replaceTopLevelBlock(
            content,
            "schemaVersion",
            CanonicalYaml.renderSection("schemaVersion", dto.schemaVersion, mapper),
        )
    content = replaceTopLevelBlock(content, "project", CanonicalYaml.renderSection("project", dto.project, mapper))
    if (dto.tags != null) {
        content = replaceTopLevelBlock(content, "tags", CanonicalYaml.renderSection("tags", dto.tags, mapper))
    }
    content = replaceTopLevelBlock(content, "releases", CanonicalYaml.renderSection("releases", dto.releases, mapper))
    for (entry in dto.vulnerabilities) {
        content =
            replaceEntryById(content, parseVulnId(entry.id), serializeEntryYaml(entry, mapper), insertIfMissing = false)
    }
    return content
}

/**
 * Replaces the block of the top-level mapping key [key] (e.g. `project:`) with [newBlock], leaving
 * everything outside the block — including blank-line separators and any column-0 comment lines that
 * precede the next section — untouched. Returns [content] unchanged if [key] is not present.
 */
private fun replaceTopLevelBlock(
    content: String,
    key: String,
    newBlock: String,
): String {
    val lines = content.lines()
    val startIndex = lines.indexOfFirst { it.startsWith("$key:") }
    if (startIndex == -1) return content

    var endIndex = lines.size
    for (i in startIndex + 1 until lines.size) {
        val line = lines[i]
        if (line.isNotBlank() && !line.startsWith(" ") && !line.startsWith("-")) {
            endIndex = i
            break
        }
    }
    while (endIndex > startIndex + 1 && lines[endIndex - 1].isBlank()) {
        endIndex--
    }

    val blockLines = newBlock.lines().dropLastWhile { it.isBlank() }
    val rebuilt = lines.subList(0, startIndex) + blockLines + lines.subList(endIndex, lines.size)
    return rebuilt.joinToString("\n")
}
