// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import java.time.Instant

/** An OpenVEX document. Holds only what the specification requires, plus the products of each statement. */
data class OpenVexDocument(
    /**
     * Unique identifier of the document. Without a baseline every run mints a new one.
     */
    val id: String,
    /**
     * Author of the document, taken from the project metadata.
     */
    val author: String,
    /**
     * Time the document was issued.
     */
    val timestamp: Instant,
    /**
     * Revision of the document. Always 1 until baselines are supported.
     */
    val version: Int,
    /**
     * The statements the document makes. The specification requires at least one.
     */
    val statements: List<OpenVexStatement>,
)
