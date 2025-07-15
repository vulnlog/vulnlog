package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.common.AnalysisDataPerBranch
import dev.vulnlog.common.model.VulnlogAnalysisData

class AnalysisSplitter {
    fun filterOnReleaseBranch(analysisData: VulnlogAnalysisData?): AnalysisDataPerBranch? {
        return analysisData?.let { data -> AnalysisDataPerBranch(data.analysedAt, data.verdict, data.reasoning) }
    }
}
