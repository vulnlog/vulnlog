package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.ReleaseBranch
import dev.vulnlog.dsl.ReleaseBranchProvider.Factory.allReleases
import dev.vulnlog.dsl.ReportBuilder
import dev.vulnlog.dsl.VlAnalyseInitStep
import dev.vulnlog.dsl.VlDefaultReporter
import dev.vulnlog.dsl.VlReportInitStep
import dev.vulnlog.dsl.VlReportOnStep
import dev.vulnlog.dsl.VlReportReporterStep
import dev.vulnlog.dsl.VlReporter
import java.time.LocalDate

class VlReportInitStepImpl(private val reportBuilder: ReportBuilder) : VlReportInitStep {
    @Deprecated("Use a default reporter instead. Will be removed in upcoming releases.")
    override infix fun from(reporter: String): VlReportReporterStep {
        reportBuilder.reporter = VlDefaultReporter(reporter)
        return VlReportReporterStepImpl(reportBuilder)
    }

    override fun from(reporter: VlReporter): VlReportReporterStep {
        reportBuilder.reporter = reporter
        return VlReportReporterStepImpl(reportBuilder)
    }
}

class VlReportReporterStepImpl(private val reportBuilder: ReportBuilder) : VlReportReporterStep {
    override infix fun at(date: String): VlReportOnStep {
        reportBuilder.awareOfAt = LocalDate.parse(date)
        return VlReportOnStepImpl(reportBuilder)
    }
}

class VlReportOnStepImpl(private val reportBuilder: ReportBuilder) : VlReportOnStep {
    override infix fun on(releases: ClosedRange<ReleaseBranch>): VlAnalyseInitStep {
        val releaseList = allReleases().filter { it in releases }
        reportBuilder.affectedReleases += releaseList
        return VlAnalyseInitStepImpl(lazy { reportBuilder.build() })
    }
}
