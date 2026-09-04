// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

/** An OpenVEX document. Holds only what the specification requires, plus the products of each statement. */
data class OpenVexDocument(
    /**
     * The identity the document carries: identifier, issue time and revision.
     */
    val identity: OpenVexIdentity,
    /**
     * Author of the document, taken from the project metadata.
     */
    val author: String,
    /**
     * The statements the document makes. The specification requires at least one.
     */
    val statements: List<OpenVexStatement>,
)
