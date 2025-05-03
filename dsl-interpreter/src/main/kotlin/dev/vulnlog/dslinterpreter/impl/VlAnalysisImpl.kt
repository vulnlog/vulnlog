package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.VlAnalyseInitState
import dev.vulnlog.dsl.VlAnalyseReasoningState
import dev.vulnlog.dsl.VlAnalyseVerdictState
import dev.vulnlog.dsl.VlTaskInitState
import dev.vulnlog.dsl.VlVerdict
import dev.vulnlog.dsl.VulnlogAnalysisData
import java.time.LocalDate

data class DslAnalysisData(
    val analysedAt: LocalDate?,
    var verdict: VlVerdict?,
    var reasoning: String?,
)

class AnalysisBuilder(val dslReportData: DslReportData) {
    var analysedAt: LocalDate? = null
    var verdict: VlVerdict? = null
    var reasoning: String? = null

    fun build(): TaskBuilder {
        return TaskBuilder(DslAnalysisData(analysedAt, verdict, reasoning))
    }
}

class VlAnalyseInitStateImpl(private val analysisBuilder: Lazy<AnalysisBuilder>) : VlAnalyseInitState {
    override infix fun analysedAt(date: String): VlAnalyseVerdictState {
        analysisBuilder.value.analysedAt = LocalDate.parse(date)
        return VlAnalyseVerdictStateImpl(analysisBuilder)
    }

    override fun verdict(verdict: VlVerdict): VlAnalyseReasoningState {
        analysisBuilder.value.analysedAt = analysisBuilder.value.dslReportData.awareOfAt
        analysisBuilder.value.verdict = verdict
        return VlAnalyseReasoningStateImpl(analysisBuilder)
    }
}

class VlAnalyseVerdictStateImpl(private val analysisBuilder: Lazy<AnalysisBuilder>) : VlAnalyseVerdictState {
    override fun verdict(verdict: VlVerdict): VlAnalyseReasoningState {
        analysisBuilder.value.verdict = verdict
        return VlAnalyseReasoningStateImpl(analysisBuilder)
    }
}

class VlAnalyseReasoningStateImpl(private val analysisBuilder: Lazy<AnalysisBuilder>) : VlAnalyseReasoningState {
    override infix fun because(reasoning: String): VlTaskInitState {
        analysisBuilder.value.reasoning = reasoning
        return VlTaskInitStateImpl(lazy { analysisBuilder.value.build() })
    }
}

data class VulnlogAnalysisDataImpl(
    override val analysedAt: LocalDate,
    override val verdict: VlVerdict,
    override val reasoning: String,
) : VulnlogAnalysisData
