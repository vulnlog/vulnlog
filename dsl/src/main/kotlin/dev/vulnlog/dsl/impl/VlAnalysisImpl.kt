package dev.vulnlog.dsl.impl

import dev.vulnlog.dsl.AnalysisBuilder
import dev.vulnlog.dsl.VlAnalyseInitStep
import dev.vulnlog.dsl.VlAnalyseReasoningStep
import dev.vulnlog.dsl.VlAnalyseVerdictStep
import dev.vulnlog.dsl.VlTaskInitStep
import java.time.LocalDate

class VlAnalyseInitStepImpl(private val analysisBuilder: Lazy<AnalysisBuilder>) : VlAnalyseInitStep {
    override infix fun analysedAt(date: String): VlAnalyseVerdictStep {
        analysisBuilder.value.analysedAt = LocalDate.parse(date)
        return VlAnalyseVerdictStepImpl(analysisBuilder)
    }

    override infix fun verdict(verdict: String): VlAnalyseReasoningStep {
        analysisBuilder.value.analysedAt = analysisBuilder.value.reportData.awareOfAt
        analysisBuilder.value.verdict = verdict
        return VlAnalyseReasoningStepImpl(analysisBuilder)
    }
}

class VlAnalyseVerdictStepImpl(private val analysisBuilder: Lazy<AnalysisBuilder>) : VlAnalyseVerdictStep {
    override infix fun verdict(verdict: String): VlAnalyseReasoningStep {
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
