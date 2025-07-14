package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.ReleaseBranch
import dev.vulnlog.dsl.ReleaseBranchProvider.Factory.allReleases
import dev.vulnlog.dsl.VlAnalyseInitState
import dev.vulnlog.dsl.VlReportInitState
import dev.vulnlog.dsl.VlReportOnState
import dev.vulnlog.dsl.VlReportReporterState
import dev.vulnlog.dsl.VlReporter
import java.time.LocalDate

data class DslReportData(
    val reporters: Set<VlReporter>,
    val awareOfAt: LocalDate?,
    val affectedReleases: List<ReleaseBranch>,
)

class ReportBuilder {
    var reporter: Set<VlReporter> = emptySet()
    var awareOfAt: LocalDate? = null
    val affectedReleases: MutableList<ReleaseBranch> = mutableListOf()

    fun build(): AnalysisBuilder {
        return AnalysisBuilder(DslReportData(reporter, awareOfAt, affectedReleases))
    }
}

class VlReportInitStateImpl(private val reportBuilder: ReportBuilder) : VlReportInitState {
    override fun from(reporter: VlReporter): VlReportReporterState {
        reportBuilder.reporter = setOf(reporter)
        return VlReportReporterStateImpl(reportBuilder)
    }

    override fun from(reporters: Set<VlReporter>): VlReportReporterState {
        reportBuilder.reporter = reporters
        return VlReportReporterStateImpl(reportBuilder)
    }
}

class VlReportReporterStateImpl(private val reportBuilder: ReportBuilder) : VlReportReporterState {
    override infix fun at(date: String): VlReportOnState {
        reportBuilder.awareOfAt = LocalDate.parse(date)
        return VlReportOnStateImpl(reportBuilder)
    }
}

class VlReportOnStateImpl(private val reportBuilder: ReportBuilder) : VlReportOnState {
    override infix fun on(releases: ClosedRange<ReleaseBranch>): VlAnalyseInitState {
        val releaseList = allReleases().filter { it in releases }
        reportBuilder.affectedReleases += releaseList
        return VlAnalyseInitStateImpl(lazy { reportBuilder.build() })
    }
}
