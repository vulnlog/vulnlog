package dev.vulnlog.cli.modules

import dev.vulnlog.suppression.SuppressionCollectorService
import dev.vulnlog.suppression.SuppressionRecordTranslator
import dev.vulnlog.suppression.SuppressionVulnerabilityMapperService
import dev.vulnlog.suppression.SuppressionWriter
import dev.vulnlog.suppression.VulnerabilitySplitterService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val suppressionModule =
    module {
        single { output -> SuppressionWriter(output.get()) }
        singleOf(::SuppressionCollectorService)
        singleOf(::SuppressionRecordTranslator)
        singleOf(::SuppressionVulnerabilityMapperService)
        singleOf(::VulnerabilitySplitterService)
    }
