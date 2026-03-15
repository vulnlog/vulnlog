package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import dev.vulnlog.cli.core.init
import dev.vulnlog.cli.parse.YamlWriter
import dev.vulnlog.cli.parse.createYamlMapper
import java.io.File
import java.io.IOException
import java.nio.file.Paths

class InitCommand : CliktCommand(name = "init") {
    override fun help(context: Context): String =
        "Scaffolds a minimal Vulnlog file containing only the required sections."

    val organization: String by option(
        "--organization",
        help = "Organization name for this Vulnlog project.",
    ).required()
    val project: String by option(
        "--project",
        help = "Project name for this Vulnlog project.",
    ).required()
    val author: String by option(
        "--author",
        help = "Author name for this Vulnlog project.",
    ).required()
    val outputPath: File by option(
        "-o",
        "--output",
        help =
            """
            |Output path for the generated file.
            |If not specified, defaults to vulnlog.yaml in the current directory.
            """.trimMargin(),
    )
        .file()
        .default(Paths.get(System.getProperty("user.dir"), "vulnlog.yaml").toFile())

    private val mapper = createYamlMapper()

    override fun run() {
        val vulnlogFile = init(CURRENT_VERSION, organization, project, author)
        val content = YamlWriter.write(vulnlogFile, mapper)
        try {
            outputPath.writeText(content)
        } catch (e: IOException) {
            echo("Error writing file: ${e.message}", err = true)
            throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
        }
        echo("Vulnlog file created at: ${outputPath.toPath().toAbsolutePath()}")
    }
}
