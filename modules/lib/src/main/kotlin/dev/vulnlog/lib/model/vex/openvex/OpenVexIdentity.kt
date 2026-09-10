// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import java.time.Instant

/** The identity a document carries. A run either mints a fresh one or continues a baseline's. */
data class OpenVexIdentity(
    /**
     * Unique identifier of the document. Kept across every revision.
     */
    val id: String,
    /**
     * Time this revision was issued. Every revision carries its own.
     */
    val timestamp: Instant,
    /**
     * Revision of the document. 1 for a fresh identity, the baseline's plus 1 for a continued one.
     */
    val version: Int,
)
