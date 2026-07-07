// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.parse.YamlWriter
import dev.vulnlog.lib.parse.hasSchemaHeader
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.Severity
import tools.jackson.databind.ObjectMapper

/**
 * Rewrites a parsed schema-v1 document in the canonical style: the whole file is rendered from
 * [ParseResult.Ok.dto] (a 1:1 image of the YAML, so no field is dropped), replacing whatever
 * layout the source used. The optional `# $schema:` header is kept only when the source already
 * had it; YAML comments are not part of the format and do not survive.
 */
fun formatYaml(
    parsed: ParseResult.Ok,
    mapper: ObjectMapper,
): VulnlogFileRaw =
    VulnlogFileRaw(
        YamlWriter.renderCanonicalDocument(parsed.dto, mapper, includeSchemaHeader = hasSchemaHeader(parsed.rootNode)),
    )

sealed interface FormatOutcome {
    data object Unchanged : FormatOutcome

    data class Reformatted(
        val formatted: VulnlogFileRaw,
    ) : FormatOutcome
}

fun formatYamlOutcome(
    parsed: ParseResult.Ok,
    mapper: ObjectMapper,
): FormatOutcome {
    val formatted = formatYaml(parsed, mapper)
    return if (formatted == parsed.rawContent) FormatOutcome.Unchanged else FormatOutcome.Reformatted(formatted)
}

fun formatCommentsDroppedWarning(source: String): String =
    formatFinding(Severity.WARNING, source, message = "contains YAML comments; they are removed on write") +
        "\n" + formatHint("record notes in schema fields (e.g. comment, analysis)")
