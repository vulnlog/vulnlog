package dev.vulnlog.cli.modules

import dev.vulnlog.cli.service.RawVulnlogDslParserService
import dev.vulnlog.cli.service.VulnEntryFilterService
import dev.vulnlog.common.repository.BranchRepository
import dev.vulnlog.common.repository.ReporterRepository
import dev.vulnlog.common.repository.VulnerabilityDataRepository
import dev.vulnlog.dsl.VlDslRoot
import dev.vulnlog.dsl.VlReleasesDslRoot
import dev.vulnlog.dsl.VlVulnerabilityDslRoot
import dev.vulnlog.dslinterpreter.ScriptingHost
import dev.vulnlog.dslinterpreter.impl.VlDslRootImpl
import dev.vulnlog.dslinterpreter.impl.VlReleasesDslRootImpl
import dev.vulnlog.dslinterpreter.impl.VlVulnerabilityDslRootImpl
import dev.vulnlog.dslinterpreter.repository.BranchRepositoryImpl
import dev.vulnlog.dslinterpreter.repository.ReporterRepositoryImpl
import dev.vulnlog.dslinterpreter.repository.VulnerabilityDataRepositoryImpl
import dev.vulnlog.dslinterpreter.service.AffectedVersionsService
import dev.vulnlog.dslinterpreter.service.AffectedVersionsServiceImpl
import dev.vulnlog.dslinterpreter.service.StatusService
import dev.vulnlog.dslinterpreter.service.VulnerabilityService
import dev.vulnlog.dslinterpreter.service.VulnerabilityServiceImpl
import dev.vulnlog.dslinterpreter.splitter.AnalysisSplitter
import dev.vulnlog.dslinterpreter.splitter.ExecutionSplitter
import dev.vulnlog.dslinterpreter.splitter.InvolvedReleasesSplitter
import dev.vulnlog.dslinterpreter.splitter.ReportSplitter
import dev.vulnlog.dslinterpreter.splitter.TaskSplitter
import dev.vulnlog.dslinterpreter.splitter.VulnEntrySplitter
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val mainModule =
    module {
        single { a -> VulnEntryFilterService(a[0], a[1]) }
        singleOf(::AffectedVersionsServiceImpl) bind AffectedVersionsService::class
        singleOf(::AnalysisSplitter)
        singleOf(::BranchRepositoryImpl) bind BranchRepository::class
        singleOf(::ExecutionSplitter)
        singleOf(::InvolvedReleasesSplitter)
        singleOf(::RawVulnlogDslParserService)
        singleOf(::ReportSplitter)
        singleOf(::ReporterRepositoryImpl) bind ReporterRepository::class
        singleOf(::ScriptingHost)
        singleOf(::StatusService)
        singleOf(::TaskSplitter)
        singleOf(::VlDslRootImpl) bind VlDslRoot::class
        singleOf(::VlReleasesDslRootImpl) bind VlReleasesDslRoot::class
        singleOf(::VlVulnerabilityDslRootImpl) bind VlVulnerabilityDslRoot::class
        singleOf(::VulnEntrySplitter)
        singleOf(::VulnerabilityDataRepositoryImpl) bind VulnerabilityDataRepository::class
        singleOf(::VulnerabilityServiceImpl) bind VulnerabilityService::class
    }
