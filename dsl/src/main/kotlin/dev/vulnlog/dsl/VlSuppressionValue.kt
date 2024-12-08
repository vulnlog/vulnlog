package dev.vulnlog.dsl

interface VlSuppressionValue {
    val reporter: VlReporterValue?
    val onAllVulnerabilities: Boolean
    val genericFilters: List<String>
}
