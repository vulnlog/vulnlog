package dev.vulnlog.dsl

import java.time.LocalDate

public interface VulnlogReportData {
    public val reporters: Set<VlReporter>
    public val awareAt: LocalDate
    public val affected: List<ReleaseBranchData>
}

/**
 * Represents the initial state of a vulnerability report within the vulnerability reporting DSL.
 */
public interface VlReportInitState {
    /**
     * The [reporter] that reported the vulnerability.
     *
     * @since v0.6.0
     */
    public infix fun from(reporter: VlReporter): VlReportReporterState

    /**
     * The [reporters] that reported the vulnerability.
     *
     * @since v0.6.0
     */
    public infix fun from(reporters: Set<VlReporter>): VlReportReporterState
}

/**
 * Defines the date since when the software security engineering team is aware of this vulnerability.
 */
public interface VlReportReporterState {
    /**
     * A date string in the format YYYY-MM-dd, e.g. `2025-03-07`
     *
     * @since v0.5.0
     */
    public infix fun at(date: String): VlReportOnState
}

/**
 * Define on what release branches the reported vulnerability was found.
 */
public interface VlReportOnState {
    /**
     * A range of release branches e.g. `v1..v2`
     *
     * @since v0.5.0
     */
    public infix fun on(releases: ClosedRange<ReleaseBranch>): VlAnalyseInitState
}
