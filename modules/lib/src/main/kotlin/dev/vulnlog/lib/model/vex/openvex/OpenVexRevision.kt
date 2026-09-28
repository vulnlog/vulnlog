// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

/**
 * The revision a run writes: the first one of a new document, or the next one of a baseline. A baseline is only ever
 * continued in the format version it is written in, so that version comes from the baseline and cannot disagree.
 */
sealed interface OpenVexRevision {
    /**
     * The format version the revision is written in.
     */
    val formatVersion: OpenVexFormatVersion

    /** The first revision of a new document under [id]. */
    data class First(
        val id: OpenVexDocumentId,
        override val formatVersion: OpenVexFormatVersion = OpenVexFormatVersion.LATEST,
    ) : OpenVexRevision

    /** The revision after [baseline], keeping its identifier and its format version. */
    data class Next(
        val baseline: OpenVexBaseline,
    ) : OpenVexRevision {
        override val formatVersion: OpenVexFormatVersion get() = baseline.formatVersion
    }
}
