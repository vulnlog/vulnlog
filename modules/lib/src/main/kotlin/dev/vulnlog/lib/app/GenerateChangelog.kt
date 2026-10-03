// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.codec.changelog.ChangelogEncoder
import dev.vulnlog.lib.core.filter.FilterOutcome
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.filter.applyFilter
import dev.vulnlog.lib.core.filter.resolveFilter
import dev.vulnlog.lib.core.reporting.collectChangelogReleases
import dev.vulnlog.lib.core.reporting.sharedProject
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import dev.vulnlog.lib.model.reporting.Changelog
import dev.vulnlog.lib.model.reporting.ChangelogDetail

data class ChangelogRequest(
    val filter: FilterRequest,
    val format: ChangelogFormatRequest,
    val detail: ChangelogDetail,
)

sealed interface ChangelogOutcome {
    sealed interface Failed : ChangelogOutcome

    sealed interface Generated : ChangelogOutcome {
        val filter: ResolvedFilter
        val changelog: Changelog
        val content: String
    }

    data class Fixed(
        override val filter: ResolvedFilter,
        override val changelog: Changelog,
        override val content: String,
    ) : Generated

    /** No release in scope shipped a fix; whether to write the empty changelog anyway is the driver's call. */
    data class NothingFixed(
        override val filter: ResolvedFilter,
        override val changelog: Changelog,
        override val content: String,
    ) : Generated
}

fun generateChangelog(
    projects: List<ValidVulnlogProject>,
    request: ChangelogRequest,
): ChangelogOutcome {
    val files = projects.map { it.vulnlogProjectFile }
    val project = sharedProject(files) ?: return ProjectsDiffer(files.map { it.project }.distinct())
    val filter =
        when (val outcome = resolveFilter(request.filter, files)) {
            is FilterOutcome.Resolved -> outcome.filter
            is FilterOutcome.Rejected -> return FilterRejected(outcome.problems)
        }
    val changelog = Changelog(project, collectChangelogReleases(files.map { it.applyFilter(filter) }))
    val content =
        when (request.format) {
            ChangelogFormatRequest.Text -> ChangelogEncoder.encodeText(changelog, request.detail)
            ChangelogFormatRequest.Markdown -> ChangelogEncoder.encodeMarkdown(changelog, request.detail)
        }
    return if (changelog.releases.isEmpty()) {
        ChangelogOutcome.NothingFixed(filter, changelog, content)
    } else {
        ChangelogOutcome.Fixed(filter, changelog, content)
    }
}
