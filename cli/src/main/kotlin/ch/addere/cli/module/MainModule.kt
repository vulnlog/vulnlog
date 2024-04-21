package ch.addere.cli.module

import ch.addere.cli.command.ServiceImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val mainModule =
    module {
        singleOf(::ServiceImpl)
    }
