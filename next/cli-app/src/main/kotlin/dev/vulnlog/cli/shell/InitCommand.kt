// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.vulnlog.cli.core.init
import dev.vulnlog.cli.parse.YamlWriter
import dev.vulnlog.cli.parse.createYamlMapper
import java.io.File
import java.io.IOException

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
    val outputPath: String? by option(
        "-o",
        "--output",
        help = "Output path for the generated file. Defaults to stdout. Use '-' for explicit stdout.",
    )

    private val mapper = createYamlMapper()

    override fun run() {
        val vulnlogFile = init(CURRENT_VERSION, organization, project, author)
        val content = YamlWriter.write(vulnlogFile, mapper)
        if (outputPath == null || outputPath == "-") {
            echo(content)
        } else {
            val file = File(outputPath!!)
            try {
                file.writeText(content)
            } catch (e: IOException) {
                echo("Error writing file: ${e.message}", err = true)
                throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
            }
            echo("Vulnlog file created at: ${file.toPath().toAbsolutePath()}")
        }
    }
}
