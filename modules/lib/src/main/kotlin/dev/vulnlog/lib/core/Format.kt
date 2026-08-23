// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.parse.YamlWriter
import dev.vulnlog.lib.parse.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.parse.hasSchemaHeader
import dev.vulnlog.lib.parse.validation.ParsedVulnlogProject

/**
 * Rewrites a parsed schema-v1 document in the canonical style: the whole file is rendered from the
 * DTO (a 1:1 image of the YAML, so no field is dropped), replacing whatever layout the source used.
 * The optional `# $schema:` header is kept only when the source already had it; YAML comments are
 * not part of the format and do not survive.
 */
fun formatYaml(parsedVulnlogProject: ParsedVulnlogProject): String {
    val dto =
        when (parsedVulnlogProject.validatedDto) {
            is VulnlogFileV1Dto -> parsedVulnlogProject.validatedDto
        }
    return YamlWriter.renderCanonicalDocument(dto, hasSchemaHeader(parsedVulnlogProject.nodeTree.rootNode))
}

sealed interface FormatOutcome {
    data object Unchanged : FormatOutcome

    data class Reformatted(
        val formatted: String,
    ) : FormatOutcome
}

fun formatYamlOutcome(parsedVulnlogProject: ParsedVulnlogProject): FormatOutcome {
    val formatted = formatYaml(parsedVulnlogProject)
    return if (formatted == parsedVulnlogProject.inputDocument.content) {
        FormatOutcome.Unchanged
    } else {
        FormatOutcome.Reformatted(formatted)
    }
}

fun formatCommentsDroppedWarning(source: String): String =
    formatFinding(FindingSeverity.WARNING, source, message = "contains YAML comments; they are removed on write") +
        "\n" + formatHint("record notes in schema fields (e.g. comment, analysis)")
