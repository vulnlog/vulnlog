package dev.vulnlog.suppression

import dev.vulnlog.common.SuppressionExecution
import dev.vulnlog.common.model.BranchName
import dev.vulnlog.common.model.VulnId
import dev.vulnlog.common.model.VulnStatus
import java.time.LocalDate

data class VulnsPerBranchAndRecord(
    val releaseBranch: BranchName,
    val reporter: String,
    val vuln: Set<SuppressVulnerability>,
)

data class SuppressVulnerability(
    val id: VulnId,
    val status: VulnStatus,
    val reporter: String,
    val reportDate: LocalDate,
    val analysisReasoning: String,
    val suppressType: SuppressionExecution?,
    val suppressionStart: LocalDate,
    val suppressionEnd: LocalDate?,
)
