package dev.vulnlog.cli.modules

import dev.vulnlog.cli.commands.JsonPrinter
import dev.vulnlog.cli.commands.SerialisationTranslator
import dev.vulnlog.cli.service.StatusService
import dev.vulnlog.dsl.VlDslRoot
import dev.vulnlog.dsl.VlReleasesDslRoot
import dev.vulnlog.dsl.VlVulnerabilityDslRoot
import dev.vulnlog.dslinterpreter.ScriptingHost
import dev.vulnlog.dslinterpreter.impl.VlDslRootImpl
import dev.vulnlog.dslinterpreter.impl.VlReleasesDslRootImpl
import dev.vulnlog.dslinterpreter.impl.VlVulnerabilityDslRootImpl
import dev.vulnlog.dslinterpreter.repository.BranchRepository
import dev.vulnlog.dslinterpreter.repository.BranchRepositoryImpl
import dev.vulnlog.dslinterpreter.repository.ReporterRepository
import dev.vulnlog.dslinterpreter.repository.ReporterRepositoryImpl
import dev.vulnlog.dslinterpreter.repository.VulnerabilityDataRepository
import dev.vulnlog.dslinterpreter.repository.VulnerabilityDataRepositoryImpl
import dev.vulnlog.dslinterpreter.service.AffectedVersionsService
import dev.vulnlog.dslinterpreter.service.AffectedVersionsServiceImpl
import dev.vulnlog.dslinterpreter.service.VulnerabilityService
import dev.vulnlog.dslinterpreter.service.VulnerabilityServiceImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val mainModule =
    module {
        single { output -> JsonPrinter(output.get()) }
        single { ruleSet -> StatusService(ruleSet.get()) }
        singleOf(::AffectedVersionsServiceImpl) bind AffectedVersionsService::class
        singleOf(::BranchRepositoryImpl) bind BranchRepository::class
        singleOf(::ReporterRepositoryImpl) bind ReporterRepository::class
        singleOf(::ScriptingHost)
        singleOf(::SerialisationTranslator)
        singleOf(::VlDslRootImpl) bind VlDslRoot::class
        singleOf(::VlReleasesDslRootImpl) bind VlReleasesDslRoot::class
        singleOf(::VlVulnerabilityDslRootImpl) bind VlVulnerabilityDslRoot::class
        singleOf(::VulnerabilityDataRepositoryImpl) bind VulnerabilityDataRepository::class
        singleOf(::VulnerabilityServiceImpl) bind VulnerabilityService::class
    }
