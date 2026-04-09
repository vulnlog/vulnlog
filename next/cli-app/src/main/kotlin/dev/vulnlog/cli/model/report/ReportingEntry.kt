package dev.vulnlog.cli.model.report

import dev.vulnlog.cli.model.Release
import dev.vulnlog.cli.model.VulnId

data class ReportingEntry(
    val state: WorkState,
    val primaryId: VulnId,
    val ids: Set<VulnId>,
    val shortDescription: String?,
    val impact: Impact,
    val analysis: String?,
    val reportFor: Set<Release>,
    val fixedIn: Set<Release>,
)
