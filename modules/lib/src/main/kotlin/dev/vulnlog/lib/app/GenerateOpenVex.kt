// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.codec.openvex.OpenVexBaselineResult
import dev.vulnlog.lib.codec.openvex.OpenVexEncoder
import dev.vulnlog.lib.codec.openvex.parseOpenVexBaseline
import dev.vulnlog.lib.codec.openvex.sameOpenVexContent
import dev.vulnlog.lib.core.vex.openvex.OpenVexScopeResult
import dev.vulnlog.lib.core.vex.openvex.buildOpenVexDocument
import dev.vulnlog.lib.core.vex.openvex.carryOverOpenVexTimestamps
import dev.vulnlog.lib.core.vex.openvex.collectOpenVexStatements
import dev.vulnlog.lib.core.vex.openvex.openVexEmptyReason
import dev.vulnlog.lib.core.vex.openvex.resolveOpenVexIdentity
import dev.vulnlog.lib.core.vex.openvex.resolveOpenVexScope
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineProblem
import dev.vulnlog.lib.model.vex.openvex.OpenVexCollection
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexEmptyReason
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexRevision
import dev.vulnlog.lib.model.vex.openvex.OpenVexTooling
import java.time.Instant

/** The driver reads the baseline, draws the document id and reads the clock, so the run itself stays pure. */
data class OpenVexRequest(
    val release: String?,
    val tags: Set<String>,
    val baseline: String?,
    /** Only used when no baseline is continued. */
    val documentId: OpenVexDocumentId,
    val timestamp: Instant,
    val tooling: OpenVexTooling,
    val formatVersion: OpenVexFormatVersion = OpenVexFormatVersion.LATEST,
)

sealed interface OpenVexOutcome {
    sealed interface Failed : OpenVexOutcome

    data class BaselineRejected(
        val problem: OpenVexBaselineProblem,
    ) : Failed

    /** OpenVEX requires at least one statement, so there is nothing to write. */
    data class NoStatementApplies(
        val collection: OpenVexCollection,
        val reason: OpenVexEmptyReason,
    ) : Failed

    sealed interface Generated : OpenVexOutcome {
        val collection: OpenVexCollection
        val version: OpenVexDocumentVersion
        val content: String
    }

    data class Revised(
        override val collection: OpenVexCollection,
        val document: OpenVexDocument,
        override val content: String,
    ) : Generated {
        override val version: OpenVexDocumentVersion get() = document.identity.version
    }

    /** Only the clock moved, so the baseline's bytes and version stand and no revision is issued. */
    data class Unchanged(
        override val collection: OpenVexCollection,
        override val version: OpenVexDocumentVersion,
        override val content: String,
    ) : Generated
}

fun generateOpenVex(
    vulnlogFile: VulnlogFile,
    request: OpenVexRequest,
): OpenVexOutcome {
    val scope =
        when (val result = resolveOpenVexScope(request.release, request.tags, vulnlogFile)) {
            is OpenVexScopeResult.Resolved -> result.scope
            is OpenVexScopeResult.Rejected -> return FilterRejected(result.problems)
        }
    val baseline =
        request.baseline?.let { content ->
            when (val result = parseOpenVexBaseline(content, request.formatVersion)) {
                is OpenVexBaselineResult.Parsed -> result.baseline
                is OpenVexBaselineResult.Rejected -> return OpenVexOutcome.BaselineRejected(result.problem)
            }
        }
    val collection = collectOpenVexStatements(vulnlogFile, scope)
    if (collection.statements.isEmpty()) {
        return OpenVexOutcome.NoStatementApplies(collection, openVexEmptyReason(vulnlogFile, collection))
    }
    val revision =
        baseline?.let(OpenVexRevision::Next) ?: OpenVexRevision.First(request.documentId, request.formatVersion)
    val document =
        buildOpenVexDocument(
            project = vulnlogFile.project,
            identity = resolveOpenVexIdentity(revision, request.timestamp),
            statements = carryOverOpenVexTimestamps(collection.statements, baseline),
            tooling = request.tooling,
            formatVersion = revision.formatVersion,
        )
    val baselineContent = request.baseline
    return if (baseline != null && sameOpenVexContent(baselineContent, document)) {
        OpenVexOutcome.Unchanged(collection, baseline.version, baselineContent)
    } else {
        OpenVexOutcome.Revised(collection, document, OpenVexEncoder.encode(document))
    }
}
