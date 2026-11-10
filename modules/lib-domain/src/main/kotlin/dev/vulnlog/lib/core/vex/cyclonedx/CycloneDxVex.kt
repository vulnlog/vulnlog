// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.cyclonedx

import dev.vulnlog.lib.core.filter.resolveRelease
import dev.vulnlog.lib.core.filter.resolveTags
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.VexAuthor
import dev.vulnlog.lib.model.vex.VexDocumentVersion
import dev.vulnlog.lib.model.vex.VexSupplier
import dev.vulnlog.lib.model.vex.VexTooling
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexCollection
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexDocument
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexFormatVersion
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexIdentity
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexProduct
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexRevision
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexScope
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexVulnerability
import java.time.Instant
import java.time.temporal.ChronoUnit

/** [vulnerabilities] are passed apart from [collection], because a baseline may have changed their times. */
fun buildCycloneDxVexDocument(
    project: Project,
    identity: CycloneDxVexIdentity,
    collection: CycloneDxVexCollection,
    vulnerabilities: List<CycloneDxVexVulnerability> = collection.vulnerabilities,
    tooling: VexTooling? = null,
    formatVersion: CycloneDxVexFormatVersion = CycloneDxVexFormatVersion.LATEST,
): CycloneDxVexDocument =
    CycloneDxVexDocument(
        formatVersion = formatVersion,
        identity = identity,
        author = VexAuthor(project.author, project.contact),
        supplier = VexSupplier(project.organization),
        tooling = tooling,
        product = CycloneDxVexProduct(project.name, collection.scope.release, collection.productPurls),
        vulnerabilities = vulnerabilities,
    )

/** Cut to whole seconds, so a rerun within the same second writes the same bytes. */
fun resolveCycloneDxVexIdentity(
    revision: CycloneDxVexRevision,
    now: Instant,
): CycloneDxVexIdentity {
    val at = now.truncatedTo(ChronoUnit.SECONDS)
    return when (revision) {
        is CycloneDxVexRevision.First -> CycloneDxVexIdentity(revision.serialNumber, at, VexDocumentVersion.FIRST)
        is CycloneDxVexRevision.Next ->
            CycloneDxVexIdentity(revision.baseline.serialNumber, at, revision.baseline.version.next())
    }
}

/** The release is required, see [CycloneDxVexDocument]; like OpenVEX it selects one release and not a window. */
fun resolveCycloneDxVexScope(
    release: String,
    tags: Set<String>,
    vulnlogFile: VulnlogFile,
): CycloneDxVexScopeResult {
    val files = listOf(vulnlogFile)
    val releases = resolveRelease(release, files)
    val scopeTags = resolveTags(tags, files)
    val problems = releases.problems + scopeTags.problems
    val resolved = releases.value
    if (problems.isNotEmpty() || resolved == null) return CycloneDxVexScopeResult.Rejected(problems)
    return CycloneDxVexScopeResult.Resolved(CycloneDxVexScope(release = resolved, tags = scopeTags.value))
}
