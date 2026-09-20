// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexOutcome
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.model.vex.openvex.OpenVexTooling
import dev.vulnlog.lib.parse.vex.openvex.OpenVexReader
import dev.vulnlog.lib.parse.vex.openvex.OpenVexWriter
import java.time.Instant

/**
 * Runs the writer path over [vulnlogFile]: collects the statements, resolves the identity, builds the document and
 * decides whether the [baseline]'s bytes stand because nothing but the clock changed. Shared by the CLI and the
 * Gradle plugin. [now] becomes the document `timestamp`; [tooling] names the writer in the document.
 *
 * [formatVersion] is the OpenVEX version the document is written in. A [baseline] read in another one is rejected:
 * its identity belongs to that version's bytes, and continuing it would silently rewrite the document's format.
 */
fun generateOpenVex(
    vulnlogFile: VulnlogFile,
    scope: OpenVexScope,
    baseline: OpenVexBaseline?,
    now: Instant,
    tooling: OpenVexTooling?,
    formatVersion: OpenVexFormatVersion = OpenVexFormatVersion.LATEST,
): OpenVexOutcome {
    require(baseline == null || baseline.formatVersion == formatVersion) {
        "cannot continue an OpenVEX ${baseline?.formatVersion?.version} baseline " +
            "in an OpenVEX ${formatVersion.version} document"
    }
    val collection = collectOpenVexStatements(vulnlogFile, scope)
    if (collection.statements.isEmpty()) return OpenVexOutcome.Empty(collection)

    val document =
        buildOpenVexDocument(
            project = vulnlogFile.project,
            identity = resolveOpenVexIdentity(baseline, now),
            statements = collection.statements,
            tooling = tooling,
            formatVersion = formatVersion,
        )
    val unchanged = baseline != null && OpenVexReader.isUnchanged(baseline, document)
    return OpenVexOutcome.Generated(
        collection = collection,
        document = document,
        content = if (unchanged) baseline.content else OpenVexWriter.write(document),
        unchanged = unchanged,
    )
}
