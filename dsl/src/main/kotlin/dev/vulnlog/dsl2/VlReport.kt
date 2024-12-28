package dev.vulnlog.dsl2

interface VlReport {
    fun report(reportedFor: VlReportForContext): MyReport

    /**
     * Every vulnerability has a reporter. The origin which brought this vulnerability to attention.
     */
    fun report(vararg reportedFor: VlReportForContext): Array<MyReport>
}
