// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.vex.VexStatus
import java.time.LocalDate

/** One statement: what a vulnerability means for a set of release artifacts. */
data class OpenVexStatement(
    /**
     * The vulnerability the statement is about.
     */
    val vulnerability: OpenVexVulnerability,
    /**
     * The day the status became true, taken from the file. Null only when the entry and its release are both undated;
     * the writer then dates the statement by the document, because a statement must never inherit.
     */
    val timestamp: LocalDate?,
    /**
     * The release artifacts the statement applies to.
     */
    val products: List<Purl>,
    /**
     * The vulnerable packages inside those artifacts, sorted.
     */
    val subcomponents: List<Purl>,
    /**
     * The status of the vulnerability in those artifacts, with the text that status carries.
     */
    val status: VexStatus,
)
