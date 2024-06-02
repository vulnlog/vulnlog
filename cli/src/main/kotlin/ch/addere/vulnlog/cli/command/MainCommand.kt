package ch.addere.vulnlog.cli.command

import ch.addere.vulnlog.cli.module.mainModule
import com.github.ajalt.clikt.core.CliktCommand
import org.koin.core.context.GlobalContext.startKoin

class MainCommand : CliktCommand(
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
