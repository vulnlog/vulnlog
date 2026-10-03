// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.codec.impact.ImpactReportEncoder
import dev.vulnlog.lib.core.filter.FilterOutcome
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.applyFilter
import dev.vulnlog.lib.core.filter.resolveFilter
import dev.vulnlog.lib.core.reporting.ImpactReport
import dev.vulnlog.lib.core.reporting.collectImpactEntries
import dev.vulnlog.lib.core.reporting.mergeImpactEntries
import dev.vulnlog.lib.core.reporting.sharedProject
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.reporting.ImpactEntry
import java.time.Instant

/** The driver reads the clock and knows its version, so the run itself stays pure. */
data class ImpactReportRequest(
    val filter: FilterRequest,
    val generatedAt: Instant,
    val vulnlogVersion: String,
)

sealed interface ImpactReportOutcome {
    sealed interface Failed : ImpactReportOutcome

    data class Generated(
        val report: ImpactReport,
        /** Before merging, so the messages can tell how many rows the merge saved. */
        val collected: List<ImpactEntry>,
        val content: String,
    ) : ImpactReportOutcome
}

fun generateImpactReport(
    projects: List<ValidVulnlogProject>,
    request: ImpactReportRequest,
): ImpactReportOutcome {
    val files = projects.map { it.vulnlogProjectFile }
    val project = sharedProject(files) ?: return ProjectsDiffer(files.map { it.project }.distinct())
    val filter =
        when (val outcome = resolveFilter(request.filter, files)) {
            is FilterOutcome.Resolved -> outcome.filter
            is FilterOutcome.Rejected -> return FilterRejected(outcome.problems)
        }
    val collected = files.flatMap { file -> collectImpactEntries(file.applyFilter(filter)) }
    val report =
        ImpactReport(
            project = project,
            entries = mergeImpactEntries(collected),
            generatedAt = request.generatedAt,
            vulnlogVersion = request.vulnlogVersion,
            inputs = projects.map { it.inputDocument.filename },
            filter = filter,
            asOf = request.filter.asOf?.let(::Release),
        )
    return ImpactReportOutcome.Generated(report, collected, ImpactReportEncoder.encode(report))
}
