// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.core.vex.deriveVexStatus
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatement
import java.time.Instant

/** The OpenVEX specification this writer emits. */
const val OPEN_VEX_CONTEXT: String = "https://openvex.dev/ns/v0.2.0"

/**
 * Builds the OpenVEX document for [vulnlogFile]. The document [id] and [timestamp] are passed in so the function stays
 * pure and its output byte-stable for a given input.
 */
fun buildOpenVexDocument(
    vulnlogFile: VulnlogFile,
    id: String,
    timestamp: Instant,
): OpenVexDocument =
    OpenVexDocument(
        id = id,
        author = openVexAuthor(vulnlogFile.project),
        timestamp = timestamp,
        version = 1,
        statements = collectOpenVexStatements(vulnlogFile),
    )

/**
 * Collects one statement per vulnerability entry and release, anchored to that release's purls.
 * A release without purls carries no product and is therefore left out; identical statements collapse, and the result
 * is ordered so the same input always writes the same bytes.
 */
fun collectOpenVexStatements(vulnlogFile: VulnlogFile): List<OpenVexStatement> {
    val purlsByRelease: Map<Release, List<Purl>> =
        vulnlogFile.releases
            .associateBy(ReleaseEntry::id) { entry -> entry.purls.map { it.purl }.sortedBy(Purl::value) }
    return vulnlogFile.vulnerabilities
        .flatMap { vulnEntry -> statementsOf(vulnEntry, purlsByRelease) }
        .distinct()
        .sortedWith(compareBy({ it.vulnerability.id }, { it.products.joinToString(",", transform = Purl::value) }))
}

/** The releases [vulnEntry] speaks about that declare no purls, so a caller can name them in a warning. */
fun releasesWithoutPurls(
    vulnlogFile: VulnlogFile,
    vulnEntry: VulnerabilityEntry,
): List<Release> {
    val withPurls =
        vulnlogFile.releases
            .filter { it.purls.isNotEmpty() }
            .map(ReleaseEntry::id)
            .toSet()
    return coveredReleases(vulnEntry).filterNot { it in withPurls }
}

/** The author line of the document: the project author, with the contact in parentheses when one is recorded. */
fun openVexAuthor(project: Project): String =
    project.contact?.let { contact -> "${project.author} ($contact)" } ?: project.author

/** The OpenVEX token for a [VexStatus]. */
fun openVexStatus(status: VexStatus): String =
    when (status) {
        VexStatus.UnderInvestigation -> "under_investigation"
        VexStatus.Fixed -> "fixed"
        is VexStatus.NotAffected -> "not_affected"
        is VexStatus.Affected -> "affected"
    }

/** The OpenVEX token for a [VexJustification]. */
fun openVexJustification(justification: VexJustification): String =
    when (justification) {
        VexJustification.COMPONENT_NOT_PRESENT -> "component_not_present"
        VexJustification.INLINE_MITIGATIONS_ALREADY_EXIST -> "inline_mitigations_already_exist"
        VexJustification.VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY ->
            "vulnerable_code_cannot_be_controlled_by_adversary"

        VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH -> "vulnerable_code_not_in_execute_path"
        VexJustification.VULNERABLE_CODE_NOT_PRESENT -> "vulnerable_code_not_present"
    }

private fun statementsOf(
    vulnEntry: VulnerabilityEntry,
    purlsByRelease: Map<Release, List<Purl>>,
): List<OpenVexStatement> =
    coveredReleases(vulnEntry).mapNotNull { release ->
        purlsByRelease[release]?.takeIf { it.isNotEmpty() }?.let { products ->
            OpenVexStatement(
                vulnerability = vulnEntry.id,
                products = products,
                status = deriveVexStatus(vulnEntry, release),
            )
        }
    }

/** The releases an entry makes a statement about: the ones it affects, plus the one that fixed it. */
fun coveredReleases(vulnEntry: VulnerabilityEntry): List<Release> =
    (vulnEntry.releases + listOfNotNull(vulnEntry.resolution?.release)).distinct()
