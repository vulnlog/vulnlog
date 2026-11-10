// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.vex.VexAuthor
import dev.vulnlog.lib.model.vex.VexStatementTime
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.VexSupplier
import dev.vulnlog.lib.model.vex.VexTooling

data class OpenVexDocument(
    val formatVersion: OpenVexFormatVersion,
    val identity: OpenVexIdentity,
    val author: VexAuthor,
    val supplier: VexSupplier,
    val tooling: VexTooling?,
    /** The specification requires at least one. */
    val statements: List<OpenVexStatement>,
)

data class OpenVexStatement(
    val vulnerability: OpenVexVulnerability,
    val timestamp: VexStatementTime,
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
