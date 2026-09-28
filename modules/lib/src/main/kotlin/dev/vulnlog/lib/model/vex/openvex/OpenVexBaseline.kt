// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

/** An existing document a run continues. It is read for its identity and its statement times only. */
data class OpenVexBaseline(
    val formatVersion: OpenVexFormatVersion,
    val id: OpenVexDocumentId,
    val version: OpenVexDocumentVersion,
    /** Only the statements this writer can read back, each with the time it carries. */
    val statements: List<OpenVexStatement> = emptyList(),
)

/** Every case is rejected: continuing such a baseline would fork or corrupt the published document. */
sealed interface OpenVexBaselineProblem {
    data object NotOpenVex : OpenVexBaselineProblem

    /** [declared] is kept as written, so an unknown future version is reported as a version, not as garbage. */
    data class OtherFormatVersion(
        val declared: String,
        val required: OpenVexFormatVersion,
    ) : OpenVexBaselineProblem

    /** [value] is null when the field is missing. */
    data class InvalidIdentity(
        val field: OpenVexIdentityField,
        val value: String?,
    ) : OpenVexBaselineProblem
}

enum class OpenVexIdentityField {
    ID,
    TIMESTAMP,
    VERSION,
}
