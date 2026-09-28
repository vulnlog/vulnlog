// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import java.util.UUID

/** The namespace of the document identifiers this writer mints. */
const val OPEN_VEX_ID_PREFIX: String = "https://vulnlog.dev/vex/"

/** The role this writer plays in the life of a document. */
const val OPEN_VEX_ROLE: String = "Document Creator"

/** The document identifier under the Vulnlog namespace for [uuid]. The caller draws the UUID, so this stays pure. */
fun openVexDocumentId(uuid: UUID): OpenVexDocumentId = OpenVexDocumentId(OPEN_VEX_ID_PREFIX + uuid)

/** The OpenVEX token for a [VexStatus]. */
fun openVexStatus(status: VexStatus): String =
    when (status) {
        is VexStatus.UnderInvestigation -> "under_investigation"
        VexStatus.Fixed -> "fixed"
        is VexStatus.NotAffected -> "not_affected"
        is VexStatus.Affected -> "affected"
    }

/** The advisory page of the authority that issued [id]. */
fun openVexVulnerabilityUrl(id: VulnId): String =
    when (id) {
        is VulnId.Cve -> "https://nvd.nist.gov/vuln/detail/${id.id}"
        is VulnId.Ghsa -> "https://github.com/advisories/${id.id}"
        is VulnId.RustSec -> "https://rustsec.org/advisories/${id.id}"
        is VulnId.Snyk -> "https://security.snyk.io/vuln/${id.id}"
    }

/** The OpenVEX token for a [VexJustification]. */
fun openVexJustification(justification: VexJustification): String =
    when (justification) {
        VexJustification.COMPONENT_NOT_PRESENT -> "component_not_present"
        VexJustification.INLINE_MITIGATIONS_ALREADY_EXIST -> "inline_mitigations_already_exist"
        VexJustification.VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY ->
            "vulnerable_code_cannot_be_controlled_by_adversary"

        VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH -> "vulnerable_code_not_in_execute_path"
        VexJustification.VULNERABLE_CODE_NOT_PRESENT -> "vulnerable_code_not_present"
    }
