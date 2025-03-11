package dev.vulnlog.dsl

import java.time.LocalDate

public data class ReportData(val scanner: String, val awareOfAt: LocalDate, val affectedReleases: List<ReleaseBranch>)

public class ReportBuilder {
    public var scannerName: String? = null
    public var awareOfAt: LocalDate? = null
    public val affectedReleases: MutableList<ReleaseBranch> = mutableListOf()

    public fun build(): AnalysisBuilder {
        return AnalysisBuilder(ReportData(scannerName!!, awareOfAt!!, affectedReleases))
    }
}

public interface VlReportInitStep {
    /**
     * The reporter that found the vulnerability.
     *
     * @since v0.5.0
     */
    public infix fun from(scanner: String): VlReportReporterStep
}

public interface VlReportReporterStep {
    /**
     * A date string in the format YYYY-MM-dd, e.g. `2025-03-07`
     *
     * @since v0.5.0
     */
    public infix fun at(date: String): VlReportOnStep
}

public interface VlReportOnStep {
    /**
     * A range of release branches e.g. `v1..v2`
     *
     * @since v0.5.0
     */
    public infix fun on(releases: ClosedRange<ReleaseBranch>): VlAnalyseInitStep
}

public sealed interface VulnlogReportData {
    public val scanner: String
    public val awareAt: LocalDate
    public val affected: List<ReleaseBranchData>
}

public data class VulnlogReportDataImpl(
    override val scanner: String,
    override val awareAt: LocalDate,
    override val affected: List<ReleaseBranchData>,
) : VulnlogReportData

public object VulnlogReportDataEmpty : VulnlogReportData {
    override val scanner: String = ""
    override val awareAt: LocalDate = LocalDate.MIN
    override val affected: List<ReleaseBranchData> = emptyList()

    override fun toString(): String {
        return "VulnlogReportDataEmpty()"
    }
}
