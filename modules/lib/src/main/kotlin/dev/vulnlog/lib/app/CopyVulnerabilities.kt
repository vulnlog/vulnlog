// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.document.InputDocument
import dev.vulnlog.lib.document.copyVulnerabilitiesToFile
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import dev.vulnlog.lib.document.yaml.hasYamlComments
import dev.vulnlog.lib.model.VulnId

data class CopyRequest(
    val vulnIds: Set<VulnId>,
)

sealed interface CopyOutcome {
    sealed interface Failed : CopyOutcome

    data class IdsNotInSource(
        val source: InputDocument,
        val ids: List<VulnId>,
    ) : Failed

    data class Copied(
        val files: List<CopiedFile>,
    ) : CopyOutcome
}

data class CopiedFile(
    val document: InputDocument,
    val ids: List<VulnId>,
    val content: String,
    val commentsDropped: Boolean,
)

fun copyVulnerabilities(
    source: ValidVulnlogProject,
    destinations: List<ValidVulnlogProject>,
    request: CopyRequest,
): CopyOutcome {
    val sourceFile = source.vulnlogProjectFile
    val sourceIds = sourceFile.vulnerabilities.map { it.id }
    val missing = request.vulnIds - sourceIds.toSet()
    if (missing.isNotEmpty()) return CopyOutcome.IdsNotInSource(source.inputDocument, missing.toList())

    val ids = sourceIds.filter { it in request.vulnIds }
    return CopyOutcome.Copied(
        destinations.map { destination ->
            CopiedFile(
                document = destination.inputDocument,
                ids = ids,
                content = copyVulnerabilitiesToFile(sourceFile, destination, request.vulnIds),
                commentsDropped = hasYamlComments(destination.nodeTree.rootNode),
            )
        },
    )
}
