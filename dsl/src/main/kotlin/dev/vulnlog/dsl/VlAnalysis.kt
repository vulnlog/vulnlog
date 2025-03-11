package dev.vulnlog.dsl

import java.time.LocalDate

public class AnalysisBuilder(public val reportData: ReportData) {
    public var analysedAt: LocalDate? = null
    public var verdict: String? = null
    public var reasoning: String? = null

    public fun build(): TaskBuilder {
        return TaskBuilder(AnalysisData(analysedAt!!, verdict!!, reasoning!!))
    }
}

public data class AnalysisData(val analysedAt: LocalDate, var verdict: String, var reasoning: String)

public interface Verdict {
    /**
     * A verdict based on the analysis of the report on the software project.
     *
     * @since v0.5.0
     */
    public infix fun verdict(verdict: String): VlAnalyseReasoningStep
}

public interface VlAnalyseInitStep : Verdict {
    /**
     * A date string in the format YYYY-MM-dd, e.g. `2025-03-07`. If not specified the date of the report is used.
     *
     * @since v0.5.0
     */
    public infix fun analysedAt(date: String): VlAnalyseVerdictStep
}

public interface VlAnalyseVerdictStep : Verdict

public interface VlAnalyseReasoningStep {
    /**
     * The reasoning why the verdict was chosen.
     *
     * @param reasoning description why the analysis lead to the specified verdict.
     * @since v0.5.0
     */
    public infix fun because(reasoning: String): VlTaskInitStep
}

public sealed interface VulnlogAnalysisData {
    public val analysedAt: LocalDate
    public val verdict: String
    public val reasoning: String
}

public data class VulnlogAnalysisDataImpl(
    override val analysedAt: LocalDate,
    override val verdict: String,
    override val reasoning: String,
) : VulnlogAnalysisData

public object VulnlogAnalysisDataEmpty : VulnlogAnalysisData {
    override val analysedAt: LocalDate = LocalDate.MIN
    override val verdict: String = ""
    override val reasoning: String = ""

    override fun toString(): String {
        return "VulnlogAnalysisDataEmpty()"
    }
}
