// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.cyclonedx

import dev.vulnlog.lib.model.vex.VexDocumentVersion

/** An existing document a run continues. It is read for its identity and its analysis times only. */
data class CycloneDxVexBaseline(
    val formatVersion: CycloneDxVexFormatVersion,
    val serialNumber: CycloneDxSerialNumber,
    val version: VexDocumentVersion,
    /** Only the vulnerabilities this writer can read back, each with the time it carries. */
    val vulnerabilities: List<CycloneDxVexVulnerability> = emptyList(),
)

/** Every case is rejected: continuing such a baseline would fork or corrupt the published document. */
sealed interface CycloneDxVexBaselineProblem {
    data object NotCycloneDx : CycloneDxVexBaselineProblem

    /** [declared] is kept as written, so an unknown future version is reported as a version, not as garbage. */
    data class OtherFormatVersion(
        val declared: String,
        val required: CycloneDxVexFormatVersion,
    ) : CycloneDxVexBaselineProblem

    /** [value] is null when the field is missing. */
    data class InvalidIdentity(
        val field: CycloneDxVexIdentityField,
        val value: String?,
    ) : CycloneDxVexBaselineProblem
}

enum class CycloneDxVexIdentityField {
    SERIAL_NUMBER,
    TIMESTAMP,
    VERSION,
}
