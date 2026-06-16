// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.parse.YamlWriter
import dev.vulnlog.lib.parse.hasSchemaHeader
import dev.vulnlog.lib.parse.v1.dto.VulnlogFileV1Dto
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

/**
 * Rewrites a parsed schema-v1 document in the canonical style: the whole file is rendered
 * from its data ([VulnlogFileV1Dto], a 1:1 image of the YAML, so no field is dropped), replacing
 * whatever layout the source used. The optional `# $schema:` header is kept only when [rawContent]
 * already had it; YAML comments are not part of the format and do not survive.
 */
fun formatYaml(
    rawContent: VulnlogFileRaw,
    mapper: ObjectMapper,
): VulnlogFileRaw {
    val dto = mapper.readValue<VulnlogFileV1Dto>(rawContent.content)
    return VulnlogFileRaw(
        YamlWriter.renderCanonicalDocument(dto, mapper, includeSchemaHeader = hasSchemaHeader(rawContent)),
    )
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

fun formatCommentsDroppedWarning(source: String): String =
    "Warning: $source contains YAML comments; they are removed on write. " +
        "Record notes in schema fields (e.g. comment, analysis)."
