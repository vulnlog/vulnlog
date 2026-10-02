// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.testing.test
import dev.vulnlog.lib.render.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private class MessageProbe(
    private val verbosity: Verbosity,
    private val shown: List<Message>,
) : CliktCommand(name = "probe") {
    override fun run() {
        currentContext.obj = CliDiagnostics(verbosity) { echo(it, err = true) }
        shown.forEach { echoMessage(it) }
    }
}

class MessagesTest :
    FunSpec({

        test("echoMessage shows each message from the verbosity it needs on, on stderr") {
            val messages =
                listOf(
                    Message.Status("Wrote: vex.json"),
                    Message.Warning("releases without purls are left out"),
                    Message.Verbose("collected 1 statement"),
                    Message.Debug("skipped CVE-2026-0001"),
                )
            val verbosities = listOf(Verbosity(quiet = true), Verbosity(), Verbosity(level = 1), Verbosity(level = 2))

            val shown =
                verbosities.map { verbosity ->
                    val result = MessageProbe(verbosity, messages).test("")
                    result.stderr.lines().filter(String::isNotEmpty)
                }

            val status = "Wrote: vex.json"
            val warning = "warning: releases without purls are left out"
            val verbose = "verbose: collected 1 statement"
            val debug = "debug: skipped CVE-2026-0001"
            shown shouldBe
                listOf(
                    listOf(warning),
                    listOf(status, warning),
                    listOf(status, warning, verbose),
                    listOf(status, warning, verbose, debug),
                )
        }
    })
