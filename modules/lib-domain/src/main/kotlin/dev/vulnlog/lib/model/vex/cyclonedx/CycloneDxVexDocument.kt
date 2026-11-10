// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.cyclonedx

import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.vex.VexAuthor
import dev.vulnlog.lib.model.vex.VexStatementTime
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.VexSupplier
import dev.vulnlog.lib.model.vex.VexTooling

/**
 * A standalone CycloneDX BOM that carries vulnerabilities and their analysis only. It describes one release: CycloneDX
 * holds one analysis per vulnerability, so a document cannot say that a package is fixed in one release and affected
 * in another.
 */
data class CycloneDxVexDocument(
    val formatVersion: CycloneDxVexFormatVersion,
    val identity: CycloneDxVexIdentity,
    val author: VexAuthor,
    val supplier: VexSupplier,
    val tooling: VexTooling?,
    val product: CycloneDxVexProduct,
    /** The specification allows none, but a consumer applies nothing from an empty list. */
    val vulnerabilities: List<CycloneDxVexVulnerability>,
)

/** The release the document is about, named after the project, with the release purls in scope as its parts. */
data class CycloneDxVexProduct(
    val name: String,
    val release: Release,
    val purls: List<Purl>,
) {
    init {
        require(name.isNotBlank()) { "VEX product name must not be blank" }
    }
}

data class CycloneDxVexVulnerability(
    val id: VulnId,
    val aliases: List<VulnId>,
    val description: String?,
    /** The vulnerable packages the analysis applies to; a consumer matches them against its own components. */
    val packages: List<Purl>,
    val status: VexStatus,
    val firstIssued: VexStatementTime,
)
