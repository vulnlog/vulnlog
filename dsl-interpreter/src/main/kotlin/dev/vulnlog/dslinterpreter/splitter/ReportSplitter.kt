package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.common.ReportDataPerBranch
import dev.vulnlog.common.model.VulnlogReportData
import dev.vulnlog.dsl.ReleaseBranchData

class ReportSplitter {
    fun filterOnReleaseBranch(
        releaseBranch: ReleaseBranchData,
        reportData: VulnlogReportData,
    ): ReportDataPerBranch? {
        return reportData.let { report ->
            val relevant: List<ReleaseBranchData> = report.affected.filter { it == releaseBranch }
            if (relevant.size > 1) {
                error("Multiple vulnerability reports for the same release branch: $releaseBranch")
            } else if (relevant.isEmpty()) {
                null
            } else {
                ReportDataPerBranch(report.reporters, report.awareAt)
            }
        }
    }
}
