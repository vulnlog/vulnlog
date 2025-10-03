package dev.vulnlog.cli

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.subcommands
import dev.vulnlog.cli.commands.MainCommand
import dev.vulnlog.cli.commands.ReportCommand
import dev.vulnlog.cli.commands.SuppressCommand
import dev.vulnlog.cli.modules.mainModule
import dev.vulnlog.cli.modules.reportModule
import dev.vulnlog.cli.modules.suppressionModule
import org.koin.core.context.startKoin
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val command = MainCommand().subcommands(ReportCommand()).subcommands(SuppressCommand())
    try {
        startKoin {
            modules(
                mainModule,
                reportModule,
                suppressionModule,
            )
        }
        command.parse(args)
    } catch (e: CliktError) {
        command.echoFormattedHelp(e)
        exitProcess(e.statusCode)
    }
}
