package dev.vulnlog.cli.modules

import dev.vulnlog.suppression.SuppressionGenerator
import dev.vulnlog.suppression.SuppressionWriter
import org.koin.dsl.module

val suppressionModule =
    module {
        single { config -> SuppressionGenerator(config.get()) }
        single { output -> SuppressionWriter(output.get()) }
    }
