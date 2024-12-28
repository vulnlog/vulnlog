package dev.vulnlog.dsl2

data class VlVuln(
    val ids: List<VlVulnerabilityIdentifier>,
    val reportedFor: List<VlReportFor>,
    val rating: List<VlVulnerabilityRateContext>,
    val resolutionTask: List<VlResolutionTask>,
    val taskPlans: List<VlPlan2>,
)
