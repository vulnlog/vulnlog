// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex

import dev.vulnlog.lib.model.Release

/**
 * What a consumer of an affected product should do, shared by every VEX format. Each format words it in its own
 * vocabulary; this type only says which remediation applies.
 */
sealed interface Remediation {
    /** The risk is accepted. [fixIn] names the release a fix ships with anyway, or is null when none is planned. */
    data class RiskAccepted(
        val fixIn: Release?,
    ) : Remediation

    /** A fix is intended, but no release ships it yet. */
    data object FixPlanned : Remediation

    /** Update to [release], which ships the fix. [note] describes how the vulnerability was resolved, when recorded. */
    data class UpdateTo(
        val release: Release,
        val note: String?,
    ) : Remediation

    /** Neither an intent nor a fix is recorded. */
    data object NoneAvailable : Remediation
}
