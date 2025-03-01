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

class AnalysisInit2(private val analysisBuilder: Lazy<AnalysisBuilder>) {
    infix fun analysedAt(date: String): AnalysisInit {
        analysisBuilder.value.analysedAt = LocalDate.parse(date)
        return AnalysisInit(analysisBuilder)
    }

    infix fun verdict(verdict: String): AnalysisSecond {
        analysisBuilder.value.analysedAt = analysisBuilder.value.reportData.awareOfAt
        analysisBuilder.value.verdict = verdict
        return AnalysisSecond(analysisBuilder)
    }
}

class AnalysisInit(private val analysisBuilder: Lazy<AnalysisBuilder>) {
    infix fun verdict(verdict: String): AnalysisSecond {
        analysisBuilder.value.verdict = verdict
        return AnalysisSecond(analysisBuilder)
    }
}

class AnalysisSecond(private val analysisBuilder: Lazy<AnalysisBuilder>) {
    infix fun because(reasoning: String): TaskInit {
        analysisBuilder.value.reasoning = reasoning
        return TaskInit(lazy { analysisBuilder.value.build() })
    }
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
