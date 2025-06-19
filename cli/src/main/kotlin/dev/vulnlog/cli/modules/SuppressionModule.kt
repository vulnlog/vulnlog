package dev.vulnlog.cli.modules

import dev.vulnlog.suppression.SuppressionFilter
import dev.vulnlog.suppression.SuppressionRecordTranslator
import dev.vulnlog.suppression.SuppressionTokenReplacer
import dev.vulnlog.suppression.SuppressionVulnerabilityMapperService
import dev.vulnlog.suppression.SuppressionWriter
import dev.vulnlog.suppression.service.SuppressService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val suppressionModule =
    module {
        singleOf(::SuppressService)
        singleOf(::SuppressionFilter)
        singleOf(::SuppressionRecordTranslator)
        singleOf(::SuppressionTokenReplacer)
        singleOf(::SuppressionVulnerabilityMapperService)
        singleOf(::SuppressionWriter)
    }
