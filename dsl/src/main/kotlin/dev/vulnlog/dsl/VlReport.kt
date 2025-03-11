package dev.vulnlog.dsl

import java.time.LocalDate

data class ReportData(val scanner: String, val awareOfAt: LocalDate, val affectedReleases: List<ReleaseBranch>)

class ReportBuilder {
    var scannerName: String? = null
    var awareOfAt: LocalDate? = null
    val affectedReleases: MutableList<ReleaseBranch> = mutableListOf()

    fun build(): AnalysisBuilder {
        return AnalysisBuilder(ReportData(scannerName!!, awareOfAt!!, affectedReleases))
    }
}

interface VlReportInitStep {
    /**
     * The reporter that found the vulnerability.
     *
     * @since v0.5.0
     */
    infix fun from(scanner: String): VlReportReporterStep
}

interface VlReportReporterStep {
    /**
     * A date string in the format YYYY-MM-dd, e.g. `2025-03-07`
     *
     * @since v0.5.0
     */
    infix fun at(date: String): VlReportOnStep
}

interface VlReportOnStep {
    /**
     * A range of release branches e.g. `v1..v2`
     *
     * @since v0.5.0
     */
    infix fun on(releases: ClosedRange<ReleaseBranch>): VlAnalyseInitStep
}

sealed interface VulnlogReportData {
    val scanner: String
    val awareAt: LocalDate
    val affected: List<ReleaseBranchData>
}

data class VulnlogReportDataImpl(
    override val scanner: String,
    override val awareAt: LocalDate,
    override val affected: List<ReleaseBranchData>,
) : VulnlogReportData

object VulnlogReportDataEmpty : VulnlogReportData {
    override val scanner: String = ""
    override val awareAt: LocalDate = LocalDate.MIN
    override val affected: List<ReleaseBranchData> = emptyList()

    override fun toString(): String {
        return "VulnlogReportDataEmpty()"
    }
}
