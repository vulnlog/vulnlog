// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.vex.Remediation

private const val RISK_ACCEPTED = "The risk is accepted. No fix is planned."
private const val RISK_ACCEPTED_FIX_PREFIX = "The risk is accepted for this release. A fix ships with release "
private const val FIX_PLANNED = "A fix is planned but not yet available."
private const val UPDATE_PREFIX = "Update to release "
private const val NONE_AVAILABLE = "No remediation is available yet."

/** The OpenVEX `action_statement` for [remediation]. */
internal fun actionStatementOf(remediation: Remediation): String =
    when (remediation) {
        is Remediation.RiskAccepted ->
            remediation.fixIn?.let { release -> "$RISK_ACCEPTED_FIX_PREFIX${release.value}." } ?: RISK_ACCEPTED

        Remediation.FixPlanned -> FIX_PLANNED
        is Remediation.UpdateTo -> "$UPDATE_PREFIX${remediation.release.value}."
        Remediation.NoneAvailable -> NONE_AVAILABLE
    }

/** The remediation [actionStatement] words, the reverse of [actionStatementOf], or null for a text it never writes. */
internal fun remediationOf(actionStatement: String): Remediation? =
    when {
        actionStatement == RISK_ACCEPTED -> Remediation.RiskAccepted(null)
        actionStatement == FIX_PLANNED -> Remediation.FixPlanned
        actionStatement == NONE_AVAILABLE -> Remediation.NoneAvailable
        else ->
            releaseAfter(RISK_ACCEPTED_FIX_PREFIX, actionStatement)?.let(Remediation::RiskAccepted)
                ?: releaseAfter(UPDATE_PREFIX, actionStatement)?.let(Remediation::UpdateTo)
    }

/** The release that ends [sentence] when it opens with [prefix], or null. */
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
