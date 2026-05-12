// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.vulnlog.lib.core.init
import dev.vulnlog.lib.parse.YamlWriter
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.shell.FileOutputOption

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

    private val mapper = createYamlMapper()

    override fun run() {
        val vulnlogFile = init(CURRENT_VERSION, organization, project, author)
        val content = YamlWriter.write(vulnlogFile, mapper)

        when (val target = output) {
            is FileOutputOption.File ->
                writeInit(
                    { echo(it) },
                    { echo(it, err = true) },
                    target,
                    content,
                )

            is FileOutputOption.Stdout -> echo(content)
        }
    }
}
