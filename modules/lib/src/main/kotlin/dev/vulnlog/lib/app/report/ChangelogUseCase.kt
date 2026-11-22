// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app.report

import dev.vulnlog.lib.codec.changelog.ChangelogEncoder
import dev.vulnlog.lib.core.filter.FilterOutcome
import dev.vulnlog.lib.core.filter.applyFilter
import dev.vulnlog.lib.core.filter.resolveFilter
import dev.vulnlog.lib.core.reporting.ChangelogReport
import dev.vulnlog.lib.core.reporting.collectChangelogReleases
import dev.vulnlog.lib.core.reporting.sharedProject
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.reporting.Changelog

/** Generate a changelog report according to the requested configurations. */
fun generateChangelog(
    projects: List<ValidVulnlogProject>,
    request: ChangelogRequest,
): ChangelogOutcome {
    val files = projects.map { it.vulnlogProjectFile }
    val project = sharedProject(files) ?: return ChangelogProjectsDiffer(files.map { it.project }.distinct())
    val filter =
        when (val outcome = resolveFilter(request.filter, files)) {
            is FilterOutcome.Resolved -> outcome.filter
            is FilterOutcome.Rejected -> return ChangelogFilterRejected(outcome.problems)
        }
    val changelog = Changelog(project, collectChangelogReleases(files.map { it.applyFilter(filter) }))
    val content =
        when (request.format) {
            ChangelogFormat.TEXT -> ChangelogEncoder.encodeText(changelog, request.detail)
            ChangelogFormat.MARKDOWN -> ChangelogEncoder.encodeMarkdown(changelog, request.detail)
            ChangelogFormat.JSON ->
                ChangelogEncoder.encodeJson(
                    ChangelogReport(
                        changelog = changelog,
                        generatedAt = request.generatedAt,
                        vulnlogVersion = request.vulnlogVersion,
                        inputs = projects.map { it.inputDocument.filename },
                        filter = filter,
                        asOf = request.filter.asOf?.let(::Release),
                    ),
                )
        }
    return if (changelog.releases.isEmpty()) {
        ChangelogOutcome.NothingFixed(filter, changelog, content)
    } else {
        ChangelogOutcome.Fixed(filter, changelog, content)
    }
}
