// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.vex.VexStatus
import java.time.Instant
import java.time.LocalDate

data class OpenVexDocument(
    val formatVersion: OpenVexFormatVersion,
    val identity: OpenVexIdentity,
    val author: OpenVexAuthor,
    val supplier: OpenVexSupplier,
    val tooling: OpenVexTooling?,
    /** The specification requires at least one. */
    val statements: List<OpenVexStatement>,
)

data class OpenVexStatement(
    val vulnerability: OpenVexVulnerability,
    val timestamp: OpenVexStatementTime,
    val products: List<Purl>,
    /** The vulnerable packages inside every product. */
    val subcomponents: List<Purl>,
    val status: VexStatus,
)

data class OpenVexVulnerability(
    val id: VulnId,
    val aliases: List<VulnId>,
    val description: String?,
)

/**
 * The specification requires an untouched statement to keep its time across revisions. A time the file does not state
 * is therefore taken from the revision that first issued the statement and carried from then on.
 */
sealed interface OpenVexStatementTime {
    data class Stated(
        val date: LocalDate,
    ) : OpenVexStatementTime

    data class Carried(
        val at: Instant,
    ) : OpenVexStatementTime

    data object Issued : OpenVexStatementTime
}

data class OpenVexAuthor(
    val name: String,
    val contact: String? = null,
) {
    init {
        require(name.isNotBlank()) { "VEX author is required" }
        require(contact == null || contact.isNotBlank()) { "VEX author contact must not be blank" }
    }
}

@JvmInline
value class OpenVexSupplier(
    val organization: String,
) {
    init {
        require(organization.isNotBlank()) { "VEX supplier must not be empty" }
    }
}

data class OpenVexTooling(
    val platform: String,
    val version: String,
) {
    init {
        require(platform.isNotBlank()) { "VEX tooling platform must not be blank" }
        require(version.isNotBlank()) { "VEX tooling version must not be blank" }
    }
}
