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

/** An update action: the release up to the first period that ends the sentence, then the optional note. */
private val UPDATE = Regex("""^${Regex.escape(UPDATE_PREFIX)}(.+?)\.(?: (.+))?$""")

/**
 * The OpenVEX `action_statement` for [remediation]. The resolution note only ever extends an update, so a note can
 * never soften an accepted risk.
 */
internal fun actionStatementOf(remediation: Remediation): String =
    when (remediation) {
        is Remediation.RiskAccepted ->
            remediation.fixIn?.let { release -> "$RISK_ACCEPTED_FIX_PREFIX${release.value}." } ?: RISK_ACCEPTED

        Remediation.FixPlanned -> FIX_PLANNED
        is Remediation.UpdateTo ->
            listOfNotNull(
                updateSentence(remediation.release),
                remediation.note,
            ).joinToString(" ")
        Remediation.NoneAvailable -> NONE_AVAILABLE
    }

/** The remediation [actionStatement] words, the reverse of [actionStatementOf], or null for a text it never writes. */
internal fun remediationOf(actionStatement: String): Remediation? =
    when {
        actionStatement == RISK_ACCEPTED -> Remediation.RiskAccepted(null)
        actionStatement == FIX_PLANNED -> Remediation.FixPlanned
        actionStatement == NONE_AVAILABLE -> Remediation.NoneAvailable
        actionStatement.startsWith(RISK_ACCEPTED_FIX_PREFIX) && actionStatement.endsWith(".") ->
            releaseOf(actionStatement.removePrefix(RISK_ACCEPTED_FIX_PREFIX).removeSuffix("."))
                ?.let(Remediation::RiskAccepted)

        else ->
            UPDATE.matchEntire(actionStatement)?.let { match ->
                releaseOf(match.groupValues[1])?.let { release ->
                    Remediation.UpdateTo(release, match.groups[2]?.value)
                }
            }
    }

private fun updateSentence(release: Release): String = "$UPDATE_PREFIX${release.value}."

private fun releaseOf(value: String): Release? = value.takeIf(String::isNotBlank)?.let(::Release)
