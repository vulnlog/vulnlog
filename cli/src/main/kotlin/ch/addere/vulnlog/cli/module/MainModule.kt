package ch.addere.vulnlog.cli.module

import ch.addere.vulnlog.cli.command.Service
import ch.addere.vulnlog.cli.command.ServiceImpl
import ch.addere.vulnlog.cli.output.FileOutputService
import ch.addere.vulnlog.cli.output.OutputService
import ch.addere.vulnlog.cli.output.PrinterOutputService
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.dsl.module

val mainModule =
    module {
        singleOf(::ServiceImpl) { bind<Service>() }
        single<OutputService>(named("console")) { params -> PrinterOutputService(params.get()) } withOptions {
            bind<OutputService>()
        }
        single<OutputService>(named("file")) { params -> FileOutputService(params.get()) } withOptions {
            bind<OutputService>()
        }
    }
