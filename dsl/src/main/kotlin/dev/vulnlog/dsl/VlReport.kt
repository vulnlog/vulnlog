package dev.vulnlog.dsl

import dev.vulnlog.dsl.ReleaseBranch.Factory.allReleases
import java.time.LocalDate

data class ReportData(val scanner: String, val awareOfAt: LocalDate, val affectedReleases: List<ReleaseBranch>)

class ReportBuilder {
    var scannerName: String? = null
    var awareOfAt: LocalDate? = null
    val affectedReleases: MutableList<ReleaseBranch> = mutableListOf()

    fun build(): AnalysisBuilder {
        return AnalysisBuilder(ReportData(scannerName!!, awareOfAt!!, affectedReleases))
    }
}

class ReportInit(private val reportBuilder: ReportBuilder) {
    infix fun from(scanner: String): ScannerInit {
        reportBuilder.scannerName = scanner
        return ScannerInit(reportBuilder)
    }
}

class ScannerInit(private val reportBuilder: ReportBuilder) {
    infix fun at(date: String): ScannerSecond {
        reportBuilder.awareOfAt = LocalDate.parse(date)
        return ScannerSecond(reportBuilder)
    }
}

class ScannerSecond(private val reportBuilder: ReportBuilder) {
    infix fun on(releases: ClosedRange<ReleaseBranch>): AnalysisInit2 {
        val releaseList = allReleases.filter { it in releases }
        reportBuilder.affectedReleases += releaseList
        return AnalysisInit2(lazy { reportBuilder.build() })
    }
}

sealed interface VulnlogReportData {
    val scanner: String
    val awareAt: LocalDate
    val affected: List<ReleaseBranchData>
}

data class VulnlogReportDataImpl(
    override val scanner: String,
    override val awareAt: LocalDate,
    override val affected: List<ReleaseBranchData>,
) : VulnlogReportData

object VulnlogReportDataEmpty : VulnlogReportData {
    override val scanner: String = ""
    override val awareAt: LocalDate = LocalDate.MIN
    override val affected: List<ReleaseBranchData> = emptyList()

    override fun toString(): String {
        return "VulnlogReportDataEmpty()"
    }
}
