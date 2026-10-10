// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.VexJustification
import java.time.LocalDate

/**
 * The status of one vulnerability in one product, shared by every VEX format. Each variant carries exactly the fields
 * its status requires, so a statement missing a mandatory field cannot be built.
 */
sealed interface VexStatus {
    data class UnderInvestigation(
        val statusNotes: String? = null,
    ) : VexStatus

    /** Carries no analysis: it would describe the state before the fix. */
    data object Fixed : VexStatus

    data class NotAffected(
        val justification: VexJustification,
        val impactStatement: String? = null,
    ) : VexStatus

    data class Affected(
        val remediation: Remediation,
        val statusNotes: String? = null,
    ) : VexStatus
}

/** Declared in the order statements are sorted by. */
enum class VexStatusKind {
    AFFECTED,
    FIXED,
    NOT_AFFECTED,
    UNDER_INVESTIGATION,
}

/**
 * What a consumer of an affected product should do, as data rather than text: CSAF and CycloneDX name these
 * categories in their own vocabularies.
 */
sealed interface Remediation {
    /** [fixIn] is the release a fix ships with despite the accepted risk, if any. */
    data class RiskAccepted(
        val fixIn: Release?,
    ) : Remediation

    /** [fixIn] is the release a recorded fix ships with, as long as it is not published. */
    data class FixPlanned(
        val fixIn: Release?,
    ) : Remediation

    data class UpdateTo(
        val release: Release,
    ) : Remediation

    data object NoneAvailable : Remediation
}

/** [since] is null when neither the entry nor the release records a date for the status. */
data class ReleaseStatus(
    val release: Release,
    val status: VexStatus,
    val since: LocalDate?,
)
