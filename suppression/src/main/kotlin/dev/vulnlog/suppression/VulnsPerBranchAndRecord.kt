package dev.vulnlog.suppression

import dev.vulnlog.common.SuppressionExecution
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ResultStatus
import dev.vulnlog.dsl.VlReporter
import java.time.LocalDate

data class VulnsPerBranchAndRecord(
    val releaseBranch: ReleaseBranchData,
    val reporter: VlReporter,
    val vuln: Set<SuppressVulnerability>,
)

data class SuppressVulnerability(
    val id: String,
    val status: ResultStatus,
    val reporter: VlReporter,
    val reportDate: LocalDate,
    val analysisReasoning: String,
    val suppressType: SuppressionExecution?,
    val suppressionStart: LocalDate,
    val suppressionEnd: LocalDate?,
)
