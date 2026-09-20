// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

/** What reading a baseline file found. Only a [Read] one is continued. */
sealed interface OpenVexBaselineOutcome {
    /** An OpenVEX document in the required format version. */
    data class Read(
        val baseline: OpenVexBaseline,
    ) : OpenVexBaselineOutcome

    /** No OpenVEX document at all: unreadable, foreign, or missing the identity a revision continues. */
    data object NotADocument : OpenVexBaselineOutcome

    /** An OpenVEX document, but not in the format version the run writes. The two are never mixed. */
    data class OtherFormatVersion(
        /**
         * The version the file declares, written as it stands. A version this build does not know lands here too.
         */
        val declared: String,
        /**
         * The version the run required of it.
         */
        val required: OpenVexFormatVersion,
    ) : OpenVexBaselineOutcome
}
