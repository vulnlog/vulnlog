package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.unique
import com.github.ajalt.clikt.parameters.types.path
import dev.vulnlog.cli.core.SuppressionFilter
import dev.vulnlog.cli.core.collectSuppressedVulnerabilities
import dev.vulnlog.cli.core.mapToSuppression
import dev.vulnlog.cli.model.Release
import dev.vulnlog.cli.model.ReporterType
import dev.vulnlog.cli.model.Tag
import dev.vulnlog.cli.model.VulnlogFile
import dev.vulnlog.cli.parse.suppression.SuppressionWriter.writeSuppressionOutput
import dev.vulnlog.cli.result.ParseResult
import dev.vulnlog.cli.shell.shared.parseFile
import dev.vulnlog.cli.shell.shared.parseStdin
import dev.vulnlog.cli.shell.shared.validateFiles
import java.io.File
import java.nio.file.Path
import kotlin.io.path.writeText

class SuppressCommand : CliktCommand(name = "suppress") {
    override fun help(context: Context): String = "Create suppression files."

    val file: String by argument()
    val output: Path by option("--output", help = "Output directory. Defaults to current directory.")
        .path(mustExist = true, canBeDir = true, canBeFile = false)
        .default(Path.of(System.getProperty("user.dir")))

    val reporter: ReporterType? by option(
        "--reporter",
        help =
            """
            Filter on reporter.
            Supported reporters: ${ReporterType.entries.joinToString(", ") { it.name.lowercase() }}
            """.trimIndent(),
    )
        .convert { ReporterType.valueOf(it.uppercase()) }
    val releaseOption: String? by option(
        "--release",
        help = "Filter on release, include all releases up to and including that release.",
    )
    val tagsOptions: Set<String> by option(
        "--tag",
        help = "Filter on tags. Use multiple times to filter on multiple tags.",
    )
        .multiple()
        .unique()

    override fun run() {
        val parseResults = parseAndValidate()
        val vulnlogFile = parseResults.values.first().content

        val releases: Set<Release> = checkProvidedReleaseFilter(vulnlogFile)
        val tags: Set<Tag> = checkProvidedTagsFilter(vulnlogFile)

        val targetReporters =
            vulnlogFile.vulnerabilities
                .flatMap { it.reports }
                .map { it.reporter }
                .filter { reporter == null || it == reporter }
                .toSet()

        val suppressionVulns =
            collectSuppressedVulnerabilities(vulnlogFile, SuppressionFilter(releases, tags, reporter))
        val outputSuppressions = mapToSuppression(targetReporters, suppressionVulns)

        outputSuppressions.forEach { suppressionOutput ->
            val file = writeSuppressionOutput(suppressionOutput)
            val outputPath = output.resolve(file.fileName)
            outputPath.writeText(file.content)
            echo("Suppression file created at: ${outputPath.toAbsolutePath()}")
        }
    }

    private fun parseAndValidate(): Map<File, ParseResult.Ok> {
        val parseResults =
            if (file == "-") {
                parseStdin()
            } else {
                val path = Path.of(file)
                if (!path.toFile().exists()) {
                    echo("Error: Path '$file' does not exist.", err = true)
                    throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
                }
                val name = path.fileName.toString()
                if (!(name == "vulnlog.yaml" || name.endsWith(".vl.yaml") || name.endsWith(".vl.yml"))) {
                    echo("Error: file name must be [vulnlog|*.vl].[yaml|yml]: $file", err = true)
                    throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
                }
                parseFile(path)
            }
        parseResults.onEachFailure { file, result ->
            echo("Parsing of ${file.name} failed:", err = true)
            echo(result.error, err = true)
        }
        if (parseResults.failure.isNotEmpty()) {
            throw ProgramResult(ExitCode.VALIDATION_ERROR.ordinal)
        }

        val validationFindings = validateFiles(parseResults.success)
        if (validationFindings.renderedFindings.isNotBlank() && validationFindings.hasErrors) {
            echo(validationFindings.renderedFindings, err = true)
            throw ProgramResult(ExitCode.VALIDATION_ERROR.ordinal)
        }
        return parseResults.success
    }

    private fun checkProvidedReleaseFilter(vulnlogFile: VulnlogFile): Set<Release> =
        if (releaseOption != null) {
            try {
                val release = Release(releaseOption!!)
                val orderedReleases = vulnlogFile.releases.map { it.id }
                val index = orderedReleases.indexOf(release)
                if (index == -1) {
                    echo("Release not found: $releaseOption", err = true)
                    echo("Known releases: ${orderedReleases.joinToString(", ") { it.value }}", err = true)
                    throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.ordinal)
                }
                orderedReleases.take(index + 1).toSet()
            } catch (e: IllegalArgumentException) {
                echo("Invalid release: ${e.message}", err = true)
                echo("Known releases: ${vulnlogFile.releases.joinToString(", ") { it.id.value }}", err = true)
                throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.ordinal)
            }
        } else {
            emptySet()
        }

    private fun checkProvidedTagsFilter(vulnlogFile: VulnlogFile): Set<Tag> =
        if (tagsOptions.isNotEmpty()) {
            try {
                val tags = tagsOptions.map(::Tag).toSet()
                val unknownTags = tags.filter { tag -> tag !in vulnlogFile.tags.map { it.id } }.toSet()
                if (unknownTags.isNotEmpty()) {
                    echo("Tag not found: ${unknownTags.joinToString(", ") { it.value }}", err = true)
                    echo("Known tags: ${vulnlogFile.tags.joinToString(", ") { it.id.value }}", err = true)
                    throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.ordinal)
                }
                return tags
            } catch (e: IllegalArgumentException) {
                echo("Invalid tag: ${e.message}", err = true)
                echo("Known tags: ${vulnlogFile.tags.joinToString(", ") { it.id.value }}", err = true)
                throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.ordinal)
            }
        } else {
            emptySet()
        }
}
