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
import dev.vulnlog.lib.model.vex.openvex.OpenVexEmptyReason
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexRevision
import dev.vulnlog.lib.model.vex.openvex.OpenVexTooling
import java.time.Instant

/**
 * What `vex openvex` and `vulnlogOpenVex` ask for. Everything the run depends on arrives as data: the driver reads the
 * baseline, draws the identifier of a new document and reads the clock.
 */
data class OpenVexRequest(
    /**
     * The single release the document covers, or null for every release that declares purls.
     */
    val release: String?,
    /**
     * The tags a release purl must carry to become a product. Empty keeps every purl.
     */
    val tags: Set<String>,
    /**
     * The text of the document to continue, or null to issue a new one.
     */
    val baseline: String?,
    /**
     * The identifier of a new document. Unused when a baseline is continued.
     */
    val documentId: OpenVexDocumentId,
    /**
     * When the revision is issued.
     */
    val timestamp: Instant,
    /**
     * The Vulnlog that writes the document.
     */
    val tooling: OpenVexTooling,
    /**
     * The OpenVEX version the document is written in. A baseline must be in the same one.
     */
    val formatVersion: OpenVexFormatVersion = OpenVexFormatVersion.LATEST,
)

/**
 * What a run did. Every case after the scope and the baseline carries the collection, so drivers can say what it held.
 * A rejected scope is the shared [FilterRejected].
 */
sealed interface OpenVexOutcome {
    /** The baseline cannot be continued, so nothing was collected. */
    data class BaselineRejected(
        val problem: OpenVexBaselineProblem,
    ) : OpenVexOutcome

    /** Nothing to write: OpenVEX requires at least one statement. [reason] says what to change. */
    data class NoStatementApplies(
        val collection: OpenVexCollection,
        val reason: OpenVexEmptyReason,
    ) : OpenVexOutcome

    /** A new revision, and the bytes it is written in. */
    data class Revised(
        val collection: OpenVexCollection,
        val document: OpenVexDocument,
        val content: String,
    ) : OpenVexOutcome

    /** Only the clock moved: the baseline's bytes stand, and [document] is the revision that is not issued. */
    data class Unchanged(
        val collection: OpenVexCollection,
        val document: OpenVexDocument,
        val content: String,
    ) : OpenVexOutcome
}

/**
 * Runs `vex openvex` over [vulnlogFile]: resolves the scope, continues the baseline or starts a new document, collects
 * the statements, carries the baseline's time over to the undated ones it already made, builds the revision, and keeps
 * the baseline's bytes when nothing but the clock changed. Shared by the CLI and the Gradle plugin.
 */
fun generateOpenVex(
    vulnlogFile: VulnlogFile,
    request: OpenVexRequest,
): OpenVexOutcome {
    val scope =
        when (val result = resolveOpenVexScope(request.release, request.tags, vulnlogFile)) {
            is OpenVexScopeResult.Resolved -> result.scope
            is OpenVexScopeResult.Rejected -> return FilterRejected(result.problems)
        }
    val revision =
        when (val baseline = request.baseline) {
            null -> OpenVexRevision.First(request.documentId, request.formatVersion)
            else ->
                when (val result = parseOpenVexBaseline(baseline, request.formatVersion)) {
                    is OpenVexBaselineResult.Parsed -> OpenVexRevision.Next(result.baseline)
                    is OpenVexBaselineResult.Rejected -> return OpenVexOutcome.BaselineRejected(result.problem)
                }
        }
    val collection = collectOpenVexStatements(vulnlogFile, scope)
    if (collection.statements.isEmpty()) {
        return OpenVexOutcome.NoStatementApplies(collection, openVexEmptyReason(vulnlogFile, scope))
    }

    val baseline =
        when (revision) {
            is OpenVexRevision.First -> null
            is OpenVexRevision.Next -> revision.baseline
        }
    val document =
        buildOpenVexDocument(
            project = vulnlogFile.project,
            identity = resolveOpenVexIdentity(revision, request.timestamp),
            statements = carryOverOpenVexTimestamps(collection.statements, baseline),
            tooling = request.tooling,
            formatVersion = revision.formatVersion,
        )
    val baselineContent = request.baseline
    return if (baselineContent != null && sameOpenVexContent(baselineContent, document)) {
        OpenVexOutcome.Unchanged(collection, document, baselineContent)
    } else {
        OpenVexOutcome.Revised(collection, document, OpenVexEncoder.encode(document))
    }
}
