// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.vulnlog.lib.app.InitOutcome
import dev.vulnlog.lib.app.InitRequest
import dev.vulnlog.lib.app.initDocument
import dev.vulnlog.lib.io.FileOutputOption
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.StatusVerb
import dev.vulnlog.lib.render.formatStatus
import dev.vulnlog.lib.render.renderInitFailure
import dev.vulnlog.lib.render.renderWritten
import kotlin.io.path.exists

class InitCommand : CliktCommand(name = "init") {
    override fun help(context: Context): String =
        "Scaffolds a minimal Vulnlog file containing only the required sections."

    val organization: String by option(
        "--organization",
        help = "Organization name for this Vulnlog project.",
    ).required()
    val project: String by option(
        "--name",
        help = "Name for this Vulnlog project.",
    ).required()
    val author: String by option(
        "--author",
        help = "Author name for this Vulnlog project.",
    ).required()
    val output: FileOutputOption by option(
        "-o",
        "--output",
        help = "Output path for the generated file. Defaults to stdout. Use '-' for explicit stdout.",
    ).convert { toOutputFileOption(it) }
        .default(FileOutputOption.Stdout)
    val force: Boolean by option(
        "--force",
        help = "Overwrite output file if it already exists.",
    ).flag(default = false)

    override fun run() {
        val file = (output as? FileOutputOption.File)?.path
        val request =
            InitRequest(
                organization = organization,
                name = project,
                author = author,
                targetExists = file?.exists() ?: false,
                force = force,
            )

        when (val outcome = initDocument(request)) {
            is InitOutcome.Failed -> failWith(listOf(renderInitFailure(outcome, "$file", "--force")), exitCode(outcome))
            is InitOutcome.Created -> write(outcome.content)
        }
    }

    private fun write(content: String) {
        when (val target = output) {
            is FileOutputOption.File -> {
                writeOrFail(writeOutput(target.path, content))
                echoMessage(Message.Status(formatStatus(StatusVerb.CREATED, target.path.toString())))
                echoMessage(renderWritten(target.path.toString()))
            }

            is FileOutputOption.Stdout -> echo(content)
        }
    }
}
