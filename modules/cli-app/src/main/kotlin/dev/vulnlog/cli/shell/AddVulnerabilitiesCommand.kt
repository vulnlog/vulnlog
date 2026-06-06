// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.ArgumentTransformContext
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.unique
import com.github.packageurl.PackageURL
import dev.vulnlog.lib.core.AddVulnerabilityOptions
import dev.vulnlog.lib.core.addVulnerabilityToFile
import dev.vulnlog.lib.core.createVulnerabilityEntry
import dev.vulnlog.lib.core.formatAddOutcomeMessage
import dev.vulnlog.lib.core.parsePurl
import dev.vulnlog.lib.core.parseReporter
import dev.vulnlog.lib.core.parseVulnId
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.shell.FileInputOption
import kotlin.io.path.readText
import kotlin.io.path.writeText

class AddVulnerabilitiesCommand : CliktCommand(name = "vulnerability") {
    override fun help(context: Context): String =
        """
        |Add a new vulnerability entry to one or more Vulnlog files.
        |If no Vulnlog file is specified, the entry is printed to STDOUT.
        """.trimMargin()

    val destinations: List<FileInputOption.File> by argument(
        help =
            """
            Target Vulnlog file(s) to create the vulnerability in. If not specified, the entry is printed to STDOUT.
            """.trimIndent(),
    ).convert(conversion = ArgumentTransformContext::toInputFile)
        .multiple(required = false)

    val vulnId: VulnId by option(
        "--vuln-id",
        help = "Vulnerability ID for the new entry.",
    ).convert { parseVulnId(it) }
        .required()

    val releases: Set<Release> by option(
        "--release",
        help =
            """
            Release to set for the vulnerability entry (repeatable). If not specified, the latest published release is used.
            """.trimIndent(),
    ).convert { Release(it) }
        .multiple(required = false)
        .unique()

    val packages: Set<Purl> by option(
        "--package",
        help = "Package URL (PURL) the vulnerability affects (repeatable).",
    ).convert { parsePurl(PackageURL(it)) }
        .multiple(required = false)
        .unique()

    val tags: Set<Tag> by option(
        "--tag",
        help = "Tag to add to the vulnerability entry (repeatable).",
    ).convert { Tag(it) }
        .multiple(required = false)
        .unique()

    val reporter: ReporterType? by option(
        "--reporter",
        help = "Reporter of the vulnerability. The report date is set to the current date.",
    ).convert { parseReporter(it) }

    override fun run() {
        val commandOption =
            AddVulnerabilityOptions(
                vulnId = vulnId,
                releases = releases,
                packages = packages,
                tags = tags,
                reporter = reporter,
            )

        if (destinations.isEmpty()) {
            echo(createVulnerabilityEntry(commandOption))
            return
        }

        val mapper = createYamlMapper()
        for (destination in destinations) {
            val parsedDestination = parseInputOrFail(listOf(destination))
            validateParsedInputOrFailWithFailureOutput(parsedDestination)
            val vulnlogFile = parsedDestination.values.first().content

            val outcome =
                try {
                    addVulnerabilityToFile(vulnlogFile, destination.path.readText(), commandOption, mapper)
                } catch (e: IllegalArgumentException) {
                    echo("Error: ${e.message} (${destination.path})", err = true)
                    throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
                }
            destination.path.writeText(outcome.newContent)
            echo(formatAddOutcomeMessage(destination.path, outcome))
        }
    }
}
