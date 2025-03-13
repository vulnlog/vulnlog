package dev.vulnlog.dsl

import java.time.LocalDate

public data class ReportData(
    val reporter: VlReporter,
    val awareOfAt: LocalDate,
    val affectedReleases: List<ReleaseBranch>,
)

public class ReportBuilder {
    public var reporter: VlReporter? = null
    public var awareOfAt: LocalDate? = null
    public val affectedReleases: MutableList<ReleaseBranch> = mutableListOf()

    public fun build(): AnalysisBuilder {
        return AnalysisBuilder(ReportData(reporter!!, awareOfAt!!, affectedReleases))
    }
}

public interface VlReportInitStep {
    /**
     * The reporter that found the vulnerability.
     *
     * @since v0.5.0
     */
    @Deprecated("Use a default reporter instead. Will be removed in upcoming releases.")
    public infix fun from(reporter: String): VlReportReporterStep

    /**
     * The reporter that found the vulnerability.
     *
     * @since v0.6.0
     */
    public infix fun from(reporter: VlReporter): VlReportReporterStep
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
    public val reporter: VlReporter
    public val awareAt: LocalDate
    public val affected: List<ReleaseBranchData>
}

public data class VulnlogReportDataImpl(
    override val reporter: VlReporter,
    override val awareAt: LocalDate,
    override val affected: List<ReleaseBranchData>,
) : VulnlogReportData

public object VulnlogReportDataEmpty : VulnlogReportData {
    override val reporter: VlReporter = VlReporterImpl("")
    override val awareAt: LocalDate = LocalDate.MIN
    override val affected: List<ReleaseBranchData> = emptyList()

    override fun toString(): String {
        return "VulnlogReportDataEmpty()"
    }
}
