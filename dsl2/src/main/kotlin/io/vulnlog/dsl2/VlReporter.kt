package io.vulnlog.dsl2

interface VlReporter {
    /**
     * Create on or multiple reporter that can report vulnerability findings.
     *
     * @param reporterName describes a reporter reporting a vulnerability.
     * @return an array of vulnerability reporter.
     */
    fun reporter(vararg reporterName: String): Array<VlReporterValue>
}
