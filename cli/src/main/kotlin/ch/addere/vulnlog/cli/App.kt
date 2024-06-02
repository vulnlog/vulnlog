package ch.addere.vulnlog.cli

import ch.addere.vulnlog.cli.command.MainCommand
import ch.addere.vulnlog.cli.command.SuppressionCommand
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = MainCommand().subcommands(SuppressionCommand()).main(args)
