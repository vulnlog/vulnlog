// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.openvex.OpenVexOutcome
import dev.vulnlog.lib.model.vex.openvex.OpenVexRevision
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.model.vex.openvex.OpenVexTooling
import dev.vulnlog.lib.parse.vex.openvex.OpenVexReader
import dev.vulnlog.lib.parse.vex.openvex.OpenVexWriter
import java.time.Instant

/**
 * Runs the writer path over [vulnlogFile]: collects the statements, carries the baseline's time over to the undated
 * ones it already made, resolves the identity, builds the document and decides whether the baseline's bytes stand
 * because nothing but the clock changed. Shared by the CLI and the Gradle plugin.
 *
 * [revision] is the first revision of a new document or the next one of a baseline, and names the OpenVEX version the
 * document is written in. [now] becomes the document `timestamp`; [tooling] names the writer in the document.
 */
fun generateOpenVex(
    vulnlogFile: VulnlogFile,
    scope: OpenVexScope,
    revision: OpenVexRevision,
    now: Instant,
    tooling: OpenVexTooling?,
): OpenVexOutcome {
    val collection = collectOpenVexStatements(vulnlogFile, scope)
    if (collection.statements.isEmpty()) return OpenVexOutcome.Empty(collection)

    val baseline =
        when (revision) {
            is OpenVexRevision.First -> null
            is OpenVexRevision.Next -> revision.baseline
        }
    val document =
        buildOpenVexDocument(
            project = vulnlogFile.project,
            identity = resolveOpenVexIdentity(revision, now),
            statements = carryOverOpenVexTimestamps(collection.statements, baseline),
            tooling = tooling,
            formatVersion = revision.formatVersion,
        )
    val unchanged = baseline != null && OpenVexReader.isUnchanged(baseline, document)
    return OpenVexOutcome.Generated(
        collection = collection,
        document = document,
        content = if (unchanged) baseline.content else OpenVexWriter.write(document),
        unchanged = unchanged,
    )
}
