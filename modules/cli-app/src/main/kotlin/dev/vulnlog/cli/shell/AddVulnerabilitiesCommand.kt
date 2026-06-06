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
import com.github.ajalt.clikt.parameters.types.choice
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
import java.time.LocalDate
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

    val name: String? by option(
        "--name",
        help = "Common name for the vulnerability (e.g. Log4Shell).",
    )

    val aliases: Set<VulnId> by option(
        "--alias",
        help = "Alternative identifier for the entry (repeatable).",
    ).convert { parseVulnId(it) }
        .multiple(required = false)
        .unique()

    val releases: Set<Release> by option(
        "--release",
        help =
            """
            Release to set for the vulnerability entry (repeatable). If not specified, the latest release is used.
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

    val reporters: Set<ReporterType> by option(
        "--reporter",
        help = "Reporter of the vulnerability (repeatable). Each report date is set to the current date.",
    ).convert { parseReporter(it) }
        .multiple(required = false)
        .unique()

    val description: String? by option(
        "--description",
        help = "Short description of the vulnerability.",
    )

    val analysis: String? by option(
        "--analysis",
        help = "Analysis and rationale for the triage decision.",
    )

    val analyzedAt: LocalDate? by option(
        "--analyzed-at",
        help = "Date the analysis was performed (yyyy-MM-dd).",
    ).convert { LocalDate.parse(it) }

    val verdict: String? by option(
        "--verdict",
        help = "Triage verdict for the entry.",
    ).choice("affected", "not affected", "risk acceptable")

    val severity: String? by option(
        "--severity",
        help = "Severity of an affected or risk-acceptable verdict.",
    ).choice("low", "medium", "high", "critical")

    val justification: String? by option(
        "--justification",
        help = "VEX justification for a 'not affected' verdict.",
    ).choice(
        "component not present",
        "inline mitigations already exist",
        "vulnerable code cannot be controlled by adversary",
        "vulnerable code not in execute path",
        "vulnerable code not present",
    )

    val comment: String? by option(
        "--comment",
        help = "Free-text comment stored with the entry.",
    )

    override fun run() {
        val commandOption =
            AddVulnerabilityOptions(
                vulnId = vulnId,
                name = name,
                aliases = aliases,
                releases = releases,
                packages = packages,
                tags = tags,
                reporters = reporters,
                description = description,
                analysis = analysis,
                analyzedAt = analyzedAt,
                verdict = verdict,
                severity = severity,
                justification = justification,
                comment = comment,
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
