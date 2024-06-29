package io.vulnlog.cli

import com.github.ajalt.clikt.core.subcommands
import io.vulnlog.cli.command.MainCommand
import io.vulnlog.cli.command.SuppressionCommand

fun main(args: Array<String>) = MainCommand().subcommands(SuppressionCommand()).main(args)
