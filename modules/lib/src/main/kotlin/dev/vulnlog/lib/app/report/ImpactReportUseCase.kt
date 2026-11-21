// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app.report

import dev.vulnlog.lib.codec.impact.ImpactReportEncoder
import dev.vulnlog.lib.core.filter.FilterOutcome
import dev.vulnlog.lib.core.filter.applyFilter
import dev.vulnlog.lib.core.filter.resolveFilter
import dev.vulnlog.lib.core.reporting.ImpactReport
import dev.vulnlog.lib.core.reporting.collectImpactEntries
import dev.vulnlog.lib.core.reporting.mergeImpactEntries
import dev.vulnlog.lib.core.reporting.sharedProject
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import dev.vulnlog.lib.model.Release

/** Generate an impact report according to the requested configurations. */
fun generateImpactReport(
    projects: List<ValidVulnlogProject>,
    request: ImpactReportRequest,
): ImpactReportOutcome {
    val files = projects.map { it.vulnlogProjectFile }
    val project = sharedProject(files) ?: return ImpactReportProjectsDiffer(files.map { it.project }.distinct())
    val filter =
        when (val outcome = resolveFilter(request.filter, files)) {
            is FilterOutcome.Resolved -> outcome.filter
            is FilterOutcome.Rejected -> return ImpactReportFilterRejected(outcome.problems)
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

    val encodedReport =
        when (request.format) {
            ImpactReportFormat.HTML -> ImpactReportEncoder.encodeToHtml(report)
            ImpactReportFormat.JSON -> ImpactReportEncoder.encodeToJson(report)
        }

    return ImpactReportOutcome.Generated(report, collected, encodedReport)
}
