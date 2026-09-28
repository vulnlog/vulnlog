// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.vex.VexStatus

/** One statement: what a vulnerability means for a set of release artifacts. */
data class OpenVexStatement(
    /**
     * The vulnerability the statement is about.
     */
    val vulnerability: OpenVexVulnerability,
    /**
     * When the status became true: the day the file states, else the time the baseline carries for this statement,
     * else the revision that issues it. The writer always writes it out, because a statement must never inherit.
     */
    val timestamp: OpenVexStatementTime,
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
