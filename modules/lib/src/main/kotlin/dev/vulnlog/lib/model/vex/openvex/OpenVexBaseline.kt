// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

/** An existing OpenVEX document a run continues: the identity it carries, and the statements it makes. */
data class OpenVexBaseline(
    /**
     * The format version the document declares. A revision is only ever written in the same one.
     */
    val formatVersion: OpenVexFormatVersion,
    /**
     * The `@id` of the document, taken over unchanged.
     */
    val id: OpenVexDocumentId,
    /**
     * The `version` of the document. A document without one counts as 1.
     */
    val version: OpenVexDocumentVersion,
    /**
     * The statements this writer can read back from the document, each with the time it carries. An undated statement
     * equal to one of them keeps that time.
     */
    val statements: List<OpenVexStatement> = emptyList(),
)
