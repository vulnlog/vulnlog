// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.init
import dev.vulnlog.lib.document.yaml.YamlWriter
import dev.vulnlog.lib.finding.FindingSeverity
import dev.vulnlog.lib.io.FileOutputOption
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.model.SchemaVersion
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
        val vulnlogFile = init(SchemaVersion.V1, organization, project, author)
        val content = YamlWriter.write(vulnlogFile)

        when (val target = output) {
            is FileOutputOption.File -> {
                if (!force && target.path.exists()) {
                    val message = "The file ${target.path} already exists. Pass --force to replace it."
                    echoMessage(formatMessage(FindingSeverity.ERROR, message))
                    throw ProgramResult(ExitCode.GENERAL_ERROR.code)
                }
                writeOrFail(writeOutput(target.path, content))
                echoStatus(formatStatus(StatusVerb.CREATED, target.path.toString()))
                diagnosticSink().verbose("wrote ${target.path}")
            }

            is FileOutputOption.Stdout -> echo(content)
        }
    }
}
