package dev.vulnlog.cli.modules

import dev.vulnlog.report.service.HtmlReportFileWriterService
import dev.vulnlog.report.service.HtmlReportGeneratorService
import dev.vulnlog.report.service.HtmlReportService
import dev.vulnlog.report.service.JsonSerializerService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val reportModule =
    module {
        singleOf(::HtmlReportFileWriterService)
        singleOf(::HtmlReportGeneratorService)
        singleOf(::HtmlReportService)
        singleOf(::JsonSerializerService)
    }
