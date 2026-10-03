// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document

import dev.vulnlog.lib.document.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.document.validation.ParsedVulnlogProject
import dev.vulnlog.lib.document.yaml.YamlWriter
import dev.vulnlog.lib.document.yaml.hasSchemaHeader

/** Renders from the DTO, not the domain model: the DTO is a 1:1 image of the YAML, so no field is lost. */
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
