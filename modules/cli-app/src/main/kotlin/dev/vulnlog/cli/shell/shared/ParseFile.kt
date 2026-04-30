// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell.shared

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import dev.vulnlog.cli.shell.ExitCode
import dev.vulnlog.lib.parse.YamlParser
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.ParseResults
import java.io.File
import java.nio.file.Path
import kotlin.io.path.readText

fun CliktCommand.parseInputOrFail(inputs: List<FileInputOption>): Map<File, ParseResult.Ok> {
    val parseResults: ParseResults =
        try {
            parseInputs(inputs)
        } catch (e: IllegalArgumentException) {
            echo(e.message, err = true)
            throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
        } catch (e: IllegalStateException) {
            echo(e.message, err = true)
            throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
        }
    parseResults.onEachFailure { file, result ->
        echo("Parsing of ${file.name} failed:", err = true)
        echo(result.error, err = true)
    }
    if (parseResults.failure.isNotEmpty()) {
        throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
    }
    return parseResults.success
}

private fun parseInputs(inputs: List<FileInputOption>): ParseResults {
    require(inputs.isNotEmpty()) { "inputs must not be empty." }

    val parser = YamlParser(createYamlMapper())

    return if (inputs.size == 1) {
        val (file, parseResult) = parseInputOption(parser, inputs.first())
        when (parseResult) {
            is ParseResult.Error -> ParseResults(failure = mapOf(file to parseResult))
            is ParseResult.Ok -> ParseResults(success = mapOf(file to parseResult))
        }
    } else {
        val typeToInputOptions = inputs.groupBy { it::class }
        val stdinCount = typeToInputOptions[FileInputOption.Stdin::class]?.size ?: 0
        val fileCount = typeToInputOptions[FileInputOption.File::class]?.size ?: 0
        if (stdinCount > 1) {
            error("Multiple <stdin> are not supported")
        }
        if (stdinCount > 0 && fileCount > 0) {
            error("Mixing input files with STDIN is not allowed.")
        }
        parseInputOptions(parser, inputs)
    }
}

private fun parseInputOptions(
    parser: YamlParser,
    inputs: List<FileInputOption>,
): ParseResults {
    val typeToResults =
        inputs
            .map { parseInputOption(parser, it) }
            .groupBy { it.second::class }

    val success =
        typeToResults[ParseResult.Ok::class]
            ?.associate { it.first to (it.second as ParseResult.Ok) }
            ?: emptyMap()
    val failure =
        typeToResults[ParseResult.Error::class]
            ?.associate { it.first to (it.second as ParseResult.Error) }
            ?: emptyMap()

    return ParseResults(success, failure)
}

private fun parseInputOption(
    parser: YamlParser,
    input: FileInputOption,
): Pair<File, ParseResult> =
    when (input) {
        is FileInputOption.File -> Pair(input.path.toFile(), parseFile(parser, input.path))
        FileInputOption.Stdin -> Pair(File("<stdin>"), parseStdin(parser))
    }

private fun parseFile(
    parser: YamlParser,
    path: Path,
): ParseResult = parseContent(parser, path.readText())

private fun parseStdin(parser: YamlParser): ParseResult = parseContent(parser, System.`in`.bufferedReader().readText())

private fun parseContent(
    parser: YamlParser,
    content: String,
): ParseResult = parser.parse(content)

fun parseFile(path: Path): ParseResults {
    val parser = YamlParser(createYamlMapper())

    val fileToResult = mapOf(path.toFile() to parser.parse(path.readText()))

    val ok = filterByType<ParseResult.Ok>(fileToResult)
    val failure = filterByType<ParseResult.Error>(fileToResult)
    return ParseResults(ok, failure)
}

private inline fun <reified T : ParseResult> filterByType(fileToResult: Map<File, ParseResult>): Map<File, T> =
    fileToResult
        .filterValues {
            it is T
        }.mapValues { (_, result) -> result as T }
