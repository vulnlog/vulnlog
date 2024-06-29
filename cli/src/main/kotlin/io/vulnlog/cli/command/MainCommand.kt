package io.vulnlog.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import io.vulnlog.cli.module.mainModule
import org.koin.core.context.GlobalContext.startKoin

class MainCommand :
    CliktCommand(
        help =
            """
            CLI tool to interpret vulnerability log files.
            """.trimIndent(),
    ) {
    override fun run() {
        startKoin {
            modules(mainModule)
        }
    }
}
