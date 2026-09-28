// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.vex.Remediation
import dev.vulnlog.lib.model.vex.VexStatusKind
import dev.vulnlog.lib.model.vex.openvex.OpenVexAuthor
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexTooling
import java.util.UUID

// Every OpenVEX string Vulnlog writes, next to its reverse where a baseline is read back.

private const val OPEN_VEX_ID_PREFIX = "https://vulnlog.dev/vex/"

internal const val OPEN_VEX_ROLE: String = "Document Creator"

private const val CONTEXT_PREFIX = "https://openvex.dev/ns/v"
private const val UNVERSIONED_CONTEXT = "https://openvex.dev/ns"
private const val UNVERSIONED_VERSION = "0.0.1"
private const val VULNLOG_SITE = "https://vulnlog.dev/"

private const val RISK_ACCEPTED = "The risk is accepted. No fix is planned."
private const val RISK_ACCEPTED_FIX_PREFIX = "The risk is accepted for this release. A fix ships with release "
private const val FIX_PLANNED = "A fix is planned but not yet available."
private const val UPDATE_PREFIX = "Update to release "
private const val NONE_AVAILABLE = "No remediation is available yet."

/** The caller draws the UUID, so the id stays deterministic in tests. */
fun openVexDocumentId(uuid: UUID): OpenVexDocumentId = OpenVexDocumentId(OPEN_VEX_ID_PREFIX + uuid)

internal fun openVexContext(formatVersion: OpenVexFormatVersion): String = CONTEXT_PREFIX + formatVersion.version

/**
 * An unknown version still comes back, so a document of another version is told apart from another format. The
 * specification reads a context without a version as 0.0.1.
 */
internal fun declaredOpenVexVersion(context: String): String? =
    when {
        context == UNVERSIONED_CONTEXT -> UNVERSIONED_VERSION
        context.startsWith(CONTEXT_PREFIX) -> context.removePrefix(CONTEXT_PREFIX).takeIf(String::isNotBlank)
        else -> null
    }

internal fun openVexStatus(kind: VexStatusKind): String =
    when (kind) {
        VexStatusKind.AFFECTED -> "affected"
        VexStatusKind.FIXED -> "fixed"
        VexStatusKind.NOT_AFFECTED -> "not_affected"
        VexStatusKind.UNDER_INVESTIGATION -> "under_investigation"
    }

internal fun openVexStatusKind(token: String): VexStatusKind? =
    VexStatusKind.entries.firstOrNull {
        openVexStatus(it) ==
            token
    }

internal fun openVexJustification(justification: VexJustification): String =
    when (justification) {
        VexJustification.COMPONENT_NOT_PRESENT -> "component_not_present"
        VexJustification.INLINE_MITIGATIONS_ALREADY_EXIST -> "inline_mitigations_already_exist"
        VexJustification.VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY ->
            "vulnerable_code_cannot_be_controlled_by_adversary"

        VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH -> "vulnerable_code_not_in_execute_path"
        VexJustification.VULNERABLE_CODE_NOT_PRESENT -> "vulnerable_code_not_present"
    }

internal fun openVexJustificationOf(token: String): VexJustification? =
    VexJustification.entries.firstOrNull { openVexJustification(it) == token }

internal fun openVexVulnerabilityUrl(id: VulnId): String =
    when (id) {
        is VulnId.Cve -> "https://nvd.nist.gov/vuln/detail/${id.id}"
        is VulnId.Ghsa -> "https://github.com/advisories/${id.id}"
        is VulnId.RustSec -> "https://rustsec.org/advisories/${id.id}"
        is VulnId.Snyk -> "https://security.snyk.io/vuln/${id.id}"
    }

internal fun openVexAuthor(author: OpenVexAuthor): String =
    author.contact?.let { contact -> "${author.name} ($contact)" } ?: author.name

internal fun openVexTooling(tooling: OpenVexTooling): String =
    "Vulnlog ${tooling.platform} version ${tooling.version}, $VULNLOG_SITE"

internal fun openVexActionStatement(remediation: Remediation): String =
    when (remediation) {
        is Remediation.RiskAccepted ->
            remediation.fixIn?.let { release -> "$RISK_ACCEPTED_FIX_PREFIX${release.value}." } ?: RISK_ACCEPTED

        Remediation.FixPlanned -> FIX_PLANNED
        is Remediation.UpdateTo -> "$UPDATE_PREFIX${remediation.release.value}."
        Remediation.NoneAvailable -> NONE_AVAILABLE
    }

/** Null for a text this writer never writes, so a baseline statement worded elsewhere is not matched. */
internal fun openVexRemediation(actionStatement: String): Remediation? =
    when (actionStatement) {
        RISK_ACCEPTED -> Remediation.RiskAccepted(null)
        FIX_PLANNED -> Remediation.FixPlanned
        NONE_AVAILABLE -> Remediation.NoneAvailable
        else ->
            releaseAfter(RISK_ACCEPTED_FIX_PREFIX, actionStatement)?.let(Remediation::RiskAccepted)
                ?: releaseAfter(UPDATE_PREFIX, actionStatement)?.let(Remediation::UpdateTo)
    }

private fun releaseAfter(
    prefix: String,
    sentence: String,
): Release? =
    sentence
        .takeIf { it.startsWith(prefix) && it.endsWith(".") }
        ?.removePrefix(prefix)
        ?.removeSuffix(".")
        ?.takeIf(String::isNotBlank)
        ?.let(::Release)
