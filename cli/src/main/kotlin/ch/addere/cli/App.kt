package ch.addere.cli

import ch.addere.cli.command.MainCommand
import ch.addere.cli.command.SuppressionCommand
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = MainCommand().subcommands(SuppressionCommand()).main(args)
