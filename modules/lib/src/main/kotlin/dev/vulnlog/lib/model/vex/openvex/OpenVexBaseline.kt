// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import java.time.Instant

/** An existing OpenVEX document a run continues: the identity it carries, and the bytes it was read from. */
data class OpenVexBaseline(
    /**
     * The `@id` of the document, taken over unchanged.
     */
    val id: String,
    /**
     * The `timestamp` of the document, taken over unchanged.
     */
    val timestamp: Instant,
    /**
     * The `version` of the document. A document without one counts as 1.
     */
    val version: Int,
    /**
     * The bytes the document was read from, written back verbatim when nothing changed.
     */
    val content: String,
)
