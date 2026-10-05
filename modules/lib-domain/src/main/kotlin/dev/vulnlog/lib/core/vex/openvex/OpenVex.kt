// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.core.filter.resolveRelease
import dev.vulnlog.lib.core.filter.resolveTags
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.openvex.OpenVexAuthor
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentity
import dev.vulnlog.lib.model.vex.openvex.OpenVexReleaseScope
import dev.vulnlog.lib.model.vex.openvex.OpenVexRevision
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatement
import dev.vulnlog.lib.model.vex.openvex.OpenVexSupplier
import dev.vulnlog.lib.model.vex.openvex.OpenVexTooling
import java.time.Instant
import java.time.temporal.ChronoUnit

fun buildOpenVexDocument(
    project: Project,
    identity: OpenVexIdentity,
    statements: List<OpenVexStatement>,
    tooling: OpenVexTooling? = null,
    formatVersion: OpenVexFormatVersion = OpenVexFormatVersion.LATEST,
): OpenVexDocument =
    OpenVexDocument(
        formatVersion = formatVersion,
        identity = identity,
        author = OpenVexAuthor(project.author, project.contact),
        supplier = OpenVexSupplier(project.organization),
        tooling = tooling,
        statements = statements,
    )

/** Cut to whole seconds, so a rerun within the same second writes the same bytes. */
fun resolveOpenVexIdentity(
    revision: OpenVexRevision,
    now: Instant,
): OpenVexIdentity {
    val at = now.truncatedTo(ChronoUnit.SECONDS)
    return when (revision) {
        is OpenVexRevision.First -> OpenVexIdentity(revision.id, at, OpenVexDocumentVersion.FIRST)
        is OpenVexRevision.Next -> OpenVexIdentity(revision.baseline.id, at, revision.baseline.version.next())
    }
}

/** Unlike the report filters, [release] selects one release and not the window up to it. */
fun resolveOpenVexScope(
    release: String?,
    tags: Set<String>,
    vulnlogFile: VulnlogFile,
): OpenVexScopeResult {
    val files = listOf(vulnlogFile)
    val releases = resolveRelease(release, files)
    val scopeTags = resolveTags(tags, files)
    val problems = releases.problems + scopeTags.problems
    if (problems.isNotEmpty()) return OpenVexScopeResult.Rejected(problems)
    val releaseScope = releases.value?.let(OpenVexReleaseScope::Named) ?: OpenVexReleaseScope.Published
    return OpenVexScopeResult.Resolved(OpenVexScope(release = releaseScope, tags = scopeTags.value))
}
