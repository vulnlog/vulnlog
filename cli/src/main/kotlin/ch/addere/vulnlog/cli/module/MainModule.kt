package ch.addere.vulnlog.cli.module

import ch.addere.vulnlog.cli.command.Service
import ch.addere.vulnlog.cli.command.ServiceImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val mainModule =
    module {
        singleOf(::ServiceImpl) { bind<Service>() }
    }
