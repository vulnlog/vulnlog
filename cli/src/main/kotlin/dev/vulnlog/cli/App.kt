package dev.vulnlog.cli

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import dev.vulnlog.cli.commands.MainCommand
import dev.vulnlog.cli.commands.ReportCommand

fun main(args: Array<String>) = MainCommand().subcommands(ReportCommand()).main(args)
