package dev.vulnlog.common.model

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.VlReporter
import java.time.LocalDate

data class VulnlogReportData(
    val reporters: Set<VlReporter>,
    val awareAt: LocalDate,
    val affected: List<ReleaseBranchData>,
)
