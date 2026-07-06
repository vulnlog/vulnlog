// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.ByteArrayOutputStream
import java.io.PrintStream

private class BoomCommand : CliktCommand(name = "boom") {
    override fun run(): Unit = throw IllegalStateException("kaboom")
}

private class OkCommand : CliktCommand(name = "ok") {
    override fun run() = Unit
}

private fun cliWith(command: CliktCommand): VulnlogCli = VulnlogCli().subcommands(command)

private inline fun <R> withCapturedStderr(block: () -> R): Pair<R, String> {
    val original = System.err
    val buffer = ByteArrayOutputStream()
    return try {
        System.setErr(PrintStream(buffer))
        block() to buffer.toString()
    } finally {
        System.setErr(original)
    }
}

private inline fun <R> withCapturedStdout(block: () -> R): Pair<R, String> {
    val original = System.out
    val buffer = ByteArrayOutputStream()
    return try {
        System.setOut(PrintStream(buffer))
        block() to buffer.toString()
    } finally {
        System.setOut(original)
    }
}

class MainTest :
    FunSpec({

        context("renderUnexpectedError") {

            test("renders a single error line by default") {
                renderUnexpectedError(IllegalStateException("kaboom"), stackTrace = false) shouldBe
                    listOf("error: kaboom")
            }

            test("falls back to the exception class name without a message") {
                renderUnexpectedError(IllegalStateException(), stackTrace = false) shouldBe
                    listOf("error: IllegalStateException")
            }

            test("appends the stack trace when requested") {
                val lines = renderUnexpectedError(IllegalStateException("kaboom"), stackTrace = true)

                lines shouldHaveSize 2
                lines[0] shouldBe "error: kaboom"
                lines[1] shouldContain "java.lang.IllegalStateException: kaboom"
                lines[1] shouldContain "\tat "
            }
        }

        context("runVulnlog") {

            test("returns success for a completed command") {
                runVulnlog(cliWith(OkCommand()), listOf("ok")) shouldBe ExitCode.SUCCESS.ordinal
            }

            test("reports an unexpected exception as a single error line") {
                val lines = mutableListOf<String>()

                val exit = runVulnlog(cliWith(BoomCommand()), listOf("boom"), lines::add)

                exit shouldBe ExitCode.GENERAL_ERROR.ordinal
                lines shouldBe listOf("error: kaboom")
            }

            test("-vv adds the stack trace") {
                val lines = mutableListOf<String>()

                val exit = runVulnlog(cliWith(BoomCommand()), listOf("-vv", "boom"), lines::add)

                exit shouldBe ExitCode.GENERAL_ERROR.ordinal
                lines shouldHaveSize 2
                lines[0] shouldBe "error: kaboom"
                lines[1] shouldContain "IllegalStateException"
            }

            test("prints to stderr by default") {
                val (exit, stderr) =
                    withCapturedStderr {
                        runVulnlog(cliWith(BoomCommand()), listOf("boom"))
                    }

                exit shouldBe ExitCode.GENERAL_ERROR.ordinal
                stderr shouldContain "error: kaboom"
                stderr shouldNotContain "\tat "
            }

            test("unknown subcommands keep the usage error behavior") {
                val (exit, stderr) =
                    withCapturedStderr {
                        runVulnlog(cliWith(OkCommand()), listOf("nope"))
                    }

                exit shouldBe ExitCode.GENERAL_ERROR.ordinal
                stderr shouldContain "nope"
            }

            test("--help exits with success") {
                val (exit, stdout) =
                    withCapturedStdout {
                        runVulnlog(cliWith(OkCommand()), listOf("--help"))
                    }

                exit shouldBe ExitCode.SUCCESS.ordinal
                stdout shouldContain "Usage"
            }
        }
    })
