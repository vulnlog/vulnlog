package io.vulnlog.dsl

interface VlReportBy {
    /**
     * Define the reporters found the vulnerability.
     *
     * @param reporters which found the vulnerability.
     * @return reporters found the vulnerability.
     */
    fun reportBy(vararg reporters: VlReporterValue)
}
