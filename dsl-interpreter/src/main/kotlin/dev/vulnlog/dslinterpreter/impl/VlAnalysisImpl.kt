package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.VerdictSpecification
import dev.vulnlog.dsl.VlAnalyseInitStep
import dev.vulnlog.dsl.VlAnalyseReasoningStep
import dev.vulnlog.dsl.VlAnalyseVerdictStep
import dev.vulnlog.dsl.VlTaskInitStep
import dev.vulnlog.dsl.VulnlogAnalysisData
import dev.vulnlog.dsl.critical
import dev.vulnlog.dsl.high
import dev.vulnlog.dsl.low
import dev.vulnlog.dsl.moderate
import dev.vulnlog.dsl.notAffected
import java.time.LocalDate

data class AnalysisData(
    val analysedAt: LocalDate?,
    var verdict: VerdictSpecification?,
    var reasoning: String?,
)

class AnalysisBuilder(val reportData: ReportData) {
    var analysedAt: LocalDate? = null
    var verdict: VerdictSpecification? = null
    var reasoning: String? = null

    fun build(): TaskBuilder {
        return TaskBuilder(AnalysisData(analysedAt, verdict, reasoning))
    }
}

class VlAnalyseInitStepImpl(private val analysisBuilder: Lazy<AnalysisBuilder>) : VlAnalyseInitStep {
    override infix fun analysedAt(date: String): VlAnalyseVerdictStep {
        analysisBuilder.value.analysedAt = LocalDate.parse(date)
        return VlAnalyseVerdictStepImpl(analysisBuilder)
    }

    @Deprecated("The method verdict(String) is deprecated. Use the method verdict(VerdictSpecification) instead.")
    override infix fun verdict(verdict: String): VlAnalyseReasoningStep {
        analysisBuilder.value.analysedAt = analysisBuilder.value.reportData.awareOfAt
        analysisBuilder.value.verdict = getVerdictTypeHeuristically(verdict)
        return VlAnalyseReasoningStepImpl(analysisBuilder)
    }

    override fun verdict(verdict: VerdictSpecification): VlAnalyseReasoningStep {
        analysisBuilder.value.analysedAt = analysisBuilder.value.reportData.awareOfAt
        analysisBuilder.value.verdict = verdict
        return VlAnalyseReasoningStepImpl(analysisBuilder)
    }
}

class VlAnalyseVerdictStepImpl(private val analysisBuilder: Lazy<AnalysisBuilder>) : VlAnalyseVerdictStep {
    @Deprecated("The method verdict(String) is deprecated. Use the method verdict(VerdictSpecification) instead.")
    override infix fun verdict(verdict: String): VlAnalyseReasoningStep {
        analysisBuilder.value.verdict = getVerdictTypeHeuristically(verdict)
        return VlAnalyseReasoningStepImpl(analysisBuilder)
    }

    override fun verdict(verdict: VerdictSpecification): VlAnalyseReasoningStep {
        analysisBuilder.value.verdict = verdict
        return VlAnalyseReasoningStepImpl(analysisBuilder)
    }
}

class VlAnalyseReasoningStepImpl(private val analysisBuilder: Lazy<AnalysisBuilder>) : VlAnalyseReasoningStep {
    override infix fun because(reasoning: String): VlTaskInitStep {
        analysisBuilder.value.reasoning = reasoning
        return VlTaskInitStepImpl(lazy { analysisBuilder.value.build() })
    }
}

private fun getVerdictTypeHeuristically(verdict: String): VerdictSpecification {
    return when (verdict) {
        critical.level -> critical
        high.level -> high
        moderate.level -> moderate
        low.level -> low
        notAffected.level -> notAffected
        else -> error("Unknown verdict: '$verdict'")
    }
}

data class VulnlogAnalysisDataImpl(
    override val analysedAt: LocalDate,
    override val verdict: VerdictSpecification,
    override val reasoning: String,
) : VulnlogAnalysisData
