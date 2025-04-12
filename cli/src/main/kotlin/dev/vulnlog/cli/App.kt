package dev.vulnlog.cli

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.subcommands
import dev.vulnlog.cli.commands.MainCommand
import dev.vulnlog.cli.commands.ReportCommand
import dev.vulnlog.cli.modules.mainModule
import org.koin.core.context.startKoin
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val command = MainCommand().subcommands(ReportCommand())
    try {
        startKoin {
            modules(mainModule)
        }
        command.parse(args)
    } catch (e: CliktError) {
        command.echoFormattedHelp(e)
        exitProcess(e.statusCode)
    }
}
