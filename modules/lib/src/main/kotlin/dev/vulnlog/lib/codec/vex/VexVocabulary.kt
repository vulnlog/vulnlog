// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.vex

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.vex.Remediation

// The wording every VEX format shares, next to its reverse where a baseline is read back.

private const val RISK_ACCEPTED = "The risk is accepted. No fix is planned."
private const val RISK_ACCEPTED_FIX_PREFIX = "The risk is accepted for this release. A fix ships with release "
private const val FIX_PLANNED = "A fix is planned but not yet available."
private const val FIX_PLANNED_PREFIX = "A fix is planned for release "
private const val UPDATE_PREFIX = "Update to release "
private const val NONE_AVAILABLE = "No remediation is available yet."

/** The OpenVEX token of a justification; CycloneDX records it as well, because its own vocabulary is coarser. */
internal fun vexJustificationToken(justification: VexJustification): String =
    when (justification) {
        VexJustification.COMPONENT_NOT_PRESENT -> "component_not_present"
        VexJustification.INLINE_MITIGATIONS_ALREADY_EXIST -> "inline_mitigations_already_exist"
        VexJustification.VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY ->
            "vulnerable_code_cannot_be_controlled_by_adversary"

        VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH -> "vulnerable_code_not_in_execute_path"
        VexJustification.VULNERABLE_CODE_NOT_PRESENT -> "vulnerable_code_not_present"
    }

internal fun vexJustificationOf(token: String): VexJustification? =
    VexJustification.entries.firstOrNull { vexJustificationToken(it) == token }

internal fun remediationSentence(remediation: Remediation): String =
    when (remediation) {
        is Remediation.RiskAccepted ->
            remediation.fixIn?.let { release -> "$RISK_ACCEPTED_FIX_PREFIX${release.value}." } ?: RISK_ACCEPTED

        is Remediation.FixPlanned ->
            remediation.fixIn?.let { release -> "$FIX_PLANNED_PREFIX${release.value}." } ?: FIX_PLANNED

        is Remediation.UpdateTo -> "$UPDATE_PREFIX${remediation.release.value}."
        Remediation.NoneAvailable -> NONE_AVAILABLE
    }

/** Null for a text this writer never writes, so a baseline statement worded elsewhere is not matched. */
internal fun remediationOf(sentence: String): Remediation? =
    when (sentence) {
        RISK_ACCEPTED -> Remediation.RiskAccepted(null)
        FIX_PLANNED -> Remediation.FixPlanned(null)
        NONE_AVAILABLE -> Remediation.NoneAvailable
        else ->
            releaseAfter(RISK_ACCEPTED_FIX_PREFIX, sentence)?.let(Remediation::RiskAccepted)
                ?: releaseAfter(FIX_PLANNED_PREFIX, sentence)?.let(Remediation::FixPlanned)
                ?: releaseAfter(UPDATE_PREFIX, sentence)?.let(Remediation::UpdateTo)
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
