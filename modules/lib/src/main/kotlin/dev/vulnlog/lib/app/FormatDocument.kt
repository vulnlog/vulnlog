// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.document.InputDocument
import dev.vulnlog.lib.document.checkFormat
import dev.vulnlog.lib.document.formatYaml
import dev.vulnlog.lib.document.validation.ParsedVulnlogProject
import dev.vulnlog.lib.document.yaml.hasYamlComments
import dev.vulnlog.lib.finding.FormatFinding

data class FormatRequest(
    /** Report a file that is not canonical instead of rewriting it. */
    val check: Boolean,
)

sealed interface FormatOutcome {
    val document: InputDocument

    data class Unchanged(
        override val document: InputDocument,
    ) : FormatOutcome

    data class Reformatted(
        override val document: InputDocument,
        val formatted: String,
        val findings: List<FormatFinding>,
        /** The rewrite comes from the DTO, which has no place for comments. */
        val commentsDropped: Boolean,
    ) : FormatOutcome

    data class NotCanonical(
        override val document: InputDocument,
        val findings: List<FormatFinding>,
    ) : FormatOutcome
}

/** Takes the parsed project, not the valid one: a file whose domain rules fail still needs formatting to be read. */
fun formatDocument(
    project: ParsedVulnlogProject,
    request: FormatRequest,
): FormatOutcome {
    val document = project.inputDocument
    val formatted = formatYaml(project)
    return when {
        formatted == document.content -> FormatOutcome.Unchanged(document)

        request.check -> FormatOutcome.NotCanonical(document, checkFormat(project))

        else ->
            FormatOutcome.Reformatted(
                document = document,
                formatted = formatted,
                findings = checkFormat(project),
                commentsDropped = hasYamlComments(project.nodeTree.rootNode),
            )
    }
}
