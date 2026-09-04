// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexOutcome
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.parse.vex.openvex.OpenVexReader
import dev.vulnlog.lib.parse.vex.openvex.OpenVexWriter
import java.time.Instant

/**
 * Runs the writer path over [vulnlogFile]: collects the statements, resolves the identity, builds the document and
 * decides whether the [baseline]'s bytes stand because nothing but the clock changed. Shared by the CLI and the
 * Gradle plugin. [now] only ever sets the `timestamp` of a first version and `last_updated`.
 */
fun generateOpenVex(
    vulnlogFile: VulnlogFile,
    scope: OpenVexScope,
    baseline: OpenVexBaseline?,
    now: Instant,
): OpenVexOutcome {
    val collection = collectOpenVexStatements(vulnlogFile, scope)
    if (collection.statements.isEmpty()) return OpenVexOutcome.Empty(collection)

    val document =
        OpenVexDocument(
            identity = resolveOpenVexIdentity(baseline, now),
            author = openVexAuthor(vulnlogFile.project),
            statements = collection.statements,
        )
    val unchanged = baseline != null && OpenVexReader.isUnchanged(baseline, document)
    return OpenVexOutcome.Generated(
        collection = collection,
        document = document,
        content = if (unchanged) baseline.content else OpenVexWriter.write(document),
        unchanged = unchanged,
    )
}
