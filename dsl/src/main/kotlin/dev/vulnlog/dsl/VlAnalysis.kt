package dev.vulnlog.dsl

import java.time.LocalDate

class AnalysisBuilder(val reportData: ReportData) {
    var analysedAt: LocalDate? = null
    var verdict: String? = null
    var reasoning: String? = null

    fun build(): TaskBuilder {
        return TaskBuilder(AnalysisData(analysedAt!!, verdict!!, reasoning!!))
    }
}

data class AnalysisData(val analysedAt: LocalDate, var verdict: String, var reasoning: String)

interface Verdict {
    /**
     * A verdict based on the analysis of the report on the software project.
     *
     * @since v0.5.0
     */
    infix fun verdict(verdict: String): VlAnalyseReasoningStep
}

interface VlAnalyseInitStep : Verdict {
    /**
     * A date string in the format YYYY-MM-dd, e.g. `2025-03-07`. If not specified the date of the report is used.
     *
     * @since v0.5.0
     */
    infix fun analysedAt(date: String): VlAnalyseVerdictStep
}

interface VlAnalyseVerdictStep : Verdict

interface VlAnalyseReasoningStep {
    /**
     * The reasoning why the verdict was chosen.
     *
     * @param reasoning description why the analysis lead to the specified verdict.
     * @since v0.5.0
     */
    infix fun because(reasoning: String): VlTaskInitStep
}

sealed interface VulnlogAnalysisData {
    val analysedAt: LocalDate
    val verdict: String
    val reasoning: String
}

data class VulnlogAnalysisDataImpl(
    override val analysedAt: LocalDate,
    override val verdict: String,
    override val reasoning: String,
) : VulnlogAnalysisData

object VulnlogAnalysisDataEmpty : VulnlogAnalysisData {
    override val analysedAt: LocalDate = LocalDate.MIN
    override val verdict: String = ""
    override val reasoning: String = ""

    override fun toString(): String {
        return "VulnlogAnalysisDataEmpty()"
    }
}
