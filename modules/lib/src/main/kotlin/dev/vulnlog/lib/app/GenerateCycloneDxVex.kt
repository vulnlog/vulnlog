// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.codec.cyclonedx.CycloneDxVexBaselineResult
import dev.vulnlog.lib.codec.cyclonedx.CycloneDxVexEncoder
import dev.vulnlog.lib.codec.cyclonedx.parseCycloneDxVexBaseline
import dev.vulnlog.lib.codec.cyclonedx.sameCycloneDxVexContent
import dev.vulnlog.lib.core.vex.cyclonedx.CycloneDxVexScopeResult
import dev.vulnlog.lib.core.vex.cyclonedx.buildCycloneDxVexDocument
import dev.vulnlog.lib.core.vex.cyclonedx.carryOverCycloneDxVexTimes
import dev.vulnlog.lib.core.vex.cyclonedx.collectCycloneDxVexVulnerabilities
import dev.vulnlog.lib.core.vex.cyclonedx.cycloneDxVexEmptyReason
import dev.vulnlog.lib.core.vex.cyclonedx.resolveCycloneDxVexIdentity
import dev.vulnlog.lib.core.vex.cyclonedx.resolveCycloneDxVexScope
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import dev.vulnlog.lib.model.vex.VexDocumentVersion
import dev.vulnlog.lib.model.vex.VexTooling
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxSerialNumber
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexBaselineProblem
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexCollection
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexDocument
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexEmptyReason
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexFormatVersion
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexRevision
import java.time.Instant

/**
 * The driver reads the baseline, draws the serial number and reads the clock, so the run itself stays pure. Unlike
 * OpenVEX the [release] is required, see [CycloneDxVexDocument].
 */
data class CycloneDxVexRequest(
    val release: String,
    val tags: Set<String>,
    val baseline: String?,
    /** Only used when no baseline is continued. */
    val serialNumber: CycloneDxSerialNumber,
    val timestamp: Instant,
    val tooling: VexTooling,
    val formatVersion: CycloneDxVexFormatVersion = CycloneDxVexFormatVersion.LATEST,
)

sealed interface CycloneDxVexOutcome {
    sealed interface Failed : CycloneDxVexOutcome

    data class BaselineRejected(
        val problem: CycloneDxVexBaselineProblem,
    ) : Failed

    /** CycloneDX allows an empty list, but a consumer would apply nothing, so there is nothing worth writing. */
    data class NoVulnerabilityApplies(
        val collection: CycloneDxVexCollection,
        val reason: CycloneDxVexEmptyReason,
    ) : Failed

    sealed interface Generated : CycloneDxVexOutcome {
        val collection: CycloneDxVexCollection
        val version: VexDocumentVersion
        val content: String
    }

    data class Revised(
        override val collection: CycloneDxVexCollection,
        val document: CycloneDxVexDocument,
        override val content: String,
    ) : Generated {
        override val version: VexDocumentVersion get() = document.identity.version
    }

    /** Only the clock moved, so the baseline's bytes and version stand and no revision is issued. */
    data class Unchanged(
        override val collection: CycloneDxVexCollection,
        override val version: VexDocumentVersion,
        override val content: String,
    ) : Generated
}

fun generateCycloneDxVex(
    project: ValidVulnlogProject,
    request: CycloneDxVexRequest,
): CycloneDxVexOutcome {
    val vulnlogFile = project.vulnlogProjectFile
    val scope =
        when (val result = resolveCycloneDxVexScope(request.release, request.tags, vulnlogFile)) {
            is CycloneDxVexScopeResult.Resolved -> result.scope
            is CycloneDxVexScopeResult.Rejected -> return FilterRejected(result.problems)
        }
    val baseline =
        request.baseline?.let { content ->
            when (val result = parseCycloneDxVexBaseline(content, request.formatVersion)) {
                is CycloneDxVexBaselineResult.Parsed -> result.baseline
                is CycloneDxVexBaselineResult.Rejected -> return CycloneDxVexOutcome.BaselineRejected(result.problem)
            }
        }
    val collection = collectCycloneDxVexVulnerabilities(vulnlogFile, scope)
    if (collection.vulnerabilities.isEmpty()) {
        return CycloneDxVexOutcome.NoVulnerabilityApplies(collection, cycloneDxVexEmptyReason(vulnlogFile, collection))
    }
    val revision =
        baseline?.let(CycloneDxVexRevision::Next)
            ?: CycloneDxVexRevision.First(request.serialNumber, request.formatVersion)
    val document =
        buildCycloneDxVexDocument(
            project = vulnlogFile.project,
            identity = resolveCycloneDxVexIdentity(revision, request.timestamp),
            collection = collection,
            vulnerabilities = carryOverCycloneDxVexTimes(collection.vulnerabilities, baseline),
            tooling = request.tooling,
            formatVersion = revision.formatVersion,
        )
    val baselineContent = request.baseline
    return if (baseline != null && sameCycloneDxVexContent(baselineContent, document)) {
        CycloneDxVexOutcome.Unchanged(collection, baseline.version, baselineContent)
    } else {
        CycloneDxVexOutcome.Revised(collection, document, CycloneDxVexEncoder.encode(document))
    }
}
