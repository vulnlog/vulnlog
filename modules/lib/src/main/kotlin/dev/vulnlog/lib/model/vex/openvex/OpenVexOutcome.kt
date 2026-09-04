// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

/** The result of one run over a Vulnlog file. */
sealed interface OpenVexOutcome {
    val collection: OpenVexCollection

    /** Nothing to write: OpenVEX requires at least one statement. */
    data class Empty(
        override val collection: OpenVexCollection,
    ) : OpenVexOutcome

    /** The document and its bytes. [unchanged] means the bytes are the baseline's, because only the clock moved. */
    data class Generated(
        override val collection: OpenVexCollection,
        val document: OpenVexDocument,
        val content: String,
        val unchanged: Boolean,
    ) : OpenVexOutcome
}
