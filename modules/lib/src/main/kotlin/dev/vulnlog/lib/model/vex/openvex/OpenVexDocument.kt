// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

/** An OpenVEX document: its identity, who issued it, and what it states. */
data class OpenVexDocument(
    /**
     * The format version the document is written in. It picks the shape the writer emits.
     */
    val formatVersion: OpenVexFormatVersion,
    /**
     * The identity the document carries: identifier, issue time and revision.
     */
    val identity: OpenVexIdentity,
    /**
     * Author of the document, taken from the project metadata.
     */
    val author: OpenVexAuthor,
    /**
     * The organization supplying the products, written on every statement.
     */
    val supplier: OpenVexSupplier,
    /**
     * The tool and version that wrote the document, when the writer states it.
     */
    val tooling: OpenVexTooling?,
    /**
     * The statements the document makes. The specification requires at least one.
     */
    val statements: List<OpenVexStatement>,
)
