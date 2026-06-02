// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.parse.CanonicalYaml
import dev.vulnlog.lib.parse.v1.dto.VulnlogFileV1Dto
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

/**
 * Formats a Vulnlog v1 document to the canonical house style ([CanonicalYaml]) while preserving the
 * leading `# $schema:` header and any comments that sit between top-level blocks.
 *
 * Each known section (`schemaVersion`, `project`, `tags`, `releases`) and every vulnerability entry
 * is re-rendered and spliced back in place; the surrounding text — header comment, blank-line layout
 * and inter-block comments — is left untouched. Comments *inside* a re-rendered block are not kept.
 *
 * Rendering goes through the schema DTO ([VulnlogFileV1Dto]), which is a faithful 1:1 of the YAML,
 * so no field is dropped on the round-trip. The caller is expected to have parsed and validated
 * [rawContent] already; this assumes a schema v1 document.
 */
fun formatYaml(
    rawContent: String,
    mapper: ObjectMapper,
): String {
    val dto = mapper.readValue<VulnlogFileV1Dto>(rawContent)
    return formatContent(rawContent, dto, mapper)
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
        content = replaceEntryById(content, parseVulnId(entry.id), serializeEntryYaml(entry, mapper))
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

    // A block ends at the next top-level mapping key (a column-0, non-blank line). Indented lines and
    // column-0 block-sequence items ("- ...", the style where the indicator is not indented under the
    // key) belong to the current block; a column-0 comment separating sections ends it and is kept.
    var endIndex = lines.size
    for (i in startIndex + 1 until lines.size) {
        val line = lines[i]
        if (line.isNotBlank() && !line.startsWith(" ") && !line.startsWith("-")) {
            endIndex = i
            break
        }
    }
    // Keep blank separators between this block and whatever follows.
    while (endIndex > startIndex + 1 && lines[endIndex - 1].isBlank()) {
        endIndex--
    }

    val blockLines = newBlock.lines().dropLastWhile { it.isBlank() }
    val rebuilt = lines.subList(0, startIndex) + blockLines + lines.subList(endIndex, lines.size)
    return rebuilt.joinToString("\n")
}
