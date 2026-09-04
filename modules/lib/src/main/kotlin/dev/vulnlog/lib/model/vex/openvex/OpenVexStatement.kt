// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.vex.VexStatus

/** One statement: what a vulnerability means for a set of release artifacts. */
data class OpenVexStatement(
    /**
     * The vulnerability the statement is about.
     */
    val vulnerability: VulnId,
    /**
     * The release artifacts the statement applies to.
     */
    val products: List<Purl>,
    /**
     * The status of the vulnerability in those artifacts.
     */
    val status: VexStatus,
)
