package dev.vulnlog.dsl

interface VlSuppressionBuilder {
    /**
     * Specify the reporter which reported the vulnerability.
     */
    fun forReporter(reporter: VlReporterValue): VlSuppressionBuilder

    /**
     * Specify the vulnerability IDs this action is targeted for by using all vulnerability IDs.
     */
    fun onAllVulnerabilities(): VlSuppressionBuilder

    /**
     * Specify a filter condition to limit the actions effect. Filters are generic and must match the specification of
     * the reporter.
     */
    fun addFilter(genericFilter: String): VlSuppressionBuilder
}
