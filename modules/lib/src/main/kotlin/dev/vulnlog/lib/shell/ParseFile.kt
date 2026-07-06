// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import dev.vulnlog.lib.core.renderParseFailure
import dev.vulnlog.lib.core.shortenSchemaVersion
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.parseVulnlogFile
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.ParseResults
import tools.jackson.databind.ObjectMapper
import java.io.IOException
import kotlin.io.path.readText

/**
 * Reads and parses each input independently: every file progresses to its own first failing
 * pipeline step, so one run reports all files' failures together.
 */
fun parseInputs(inputs: List<FileInputOption>): ParseResults {
    require(inputs.isNotEmpty()) { "inputs must not be empty." }

    val typeToInputOptions = inputs.groupBy { it::class }
    val stdinCount = typeToInputOptions[FileInputOption.Stdin::class]?.size ?: 0
    val fileCount = typeToInputOptions[FileInputOption.File::class]?.size ?: 0
    if (stdinCount > 1) {
        error("Multiple <stdin> are not supported.")
    }
    if (stdinCount > 0 && fileCount > 0) {
        error("Mixing input files with STDIN is not allowed.")
    }

    val mapper = createYamlMapper()
    val results = inputs.associateWith { parseInput(mapper, it) }
    return ParseResults(
        success = results.mapNotNull { (input, result) -> (result as? ParseResult.Ok)?.let { input to it } }.toMap(),
        failure = results.mapNotNull { (input, result) -> (result as? ParseResult.Error)?.let { input to it } }.toMap(),
    )
}

/**
 * Renders each failure in [parseResults] as a two-line block: a header naming the file and the
 * underlying parse error. Centralises the message format so all surfaces (CLI, Gradle) report
 * parse failures identically.
 */
fun renderParseFailures(parseResults: ParseResults): List<String> =
    parseResults.failure.map { (input, result) -> renderParseFailure(input.sourceFile().name, result) }

/**
 * Renders one diagnostic line per successfully parsed input, stating the detected schema version
 * and the entry counts. Shared by the CLI and the Gradle plugin so verbose output never drifts.
 */
fun renderParsedInputs(success: Map<FileInputOption, ParseResult.Ok>): List<String> =
    success.entries
        .map { (input, result) -> renderParsedInput(input.sourceFile().name, result.content) }
        .sorted()

private fun renderParsedInput(
    name: String,
    content: VulnlogFile,
): String =
    "parsed $name: schema version ${shortenSchemaVersion(content.schemaVersion)}, " +
        "releases: ${content.releases.size}, tags: ${content.tags.size}, " +
        "vulnerabilities: ${content.vulnerabilities.size}"

private fun parseInput(
    mapper: ObjectMapper,
    input: FileInputOption,
): ParseResult {
    val content =
        try {
            when (input) {
                is FileInputOption.File -> input.path.readText()
                FileInputOption.Stdin -> System.`in`.bufferedReader().readText()
            }
        } catch (e: IOException) {
            error("Cannot read ${input.sourceFile().name}: ${e.message}")
        }
    return parseVulnlogFile(mapper, VulnlogFileRaw(content))
}
