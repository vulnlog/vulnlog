// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import java.time.Instant

/** The identity a document carries. A run either mints a fresh one or continues a baseline's. */
data class OpenVexIdentity(
    /**
     * Unique identifier of the document.
     */
    val id: String,
    /**
     * Time the document was first issued. Preserved across every continuation.
     */
    val timestamp: Instant,
    /**
     * Revision of the document. 1 for a fresh identity, the baseline's plus 1 for a continued one.
     */
    val version: Int,
    /**
     * Time this revision was written. Absent on version 1, because nothing was updated yet.
     */
    val lastUpdated: Instant? = null,
)
