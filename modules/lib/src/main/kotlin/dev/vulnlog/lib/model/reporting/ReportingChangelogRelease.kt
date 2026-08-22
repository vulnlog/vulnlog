// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.reporting

import dev.vulnlog.lib.model.Release
import java.time.LocalDate

/** The vulnerabilities one release shipped a fix for. */
data class ReportingChangelogRelease(
    val fixedIn: Release,
    /** Publication date of the release. Absence means the release is not yet published. */
    val publishedAt: LocalDate? = null,
    val summary: ChangelogSummary = ChangelogSummary(),
    val entries: List<ReportingChangelogEntry> = emptyList(),
)
