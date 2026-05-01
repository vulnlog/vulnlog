// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.shell

import dev.vulnlog.lib.parse.YamlParser
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.ParseResults
import java.io.File
import kotlin.io.path.readText

fun parseInputs(inputs: List<FileInputOption>): ParseResults {
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
        is FileInputOption.File -> Pair(input.path.toFile(), parseContent(parser, input.path.readText()))
        FileInputOption.Stdin -> Pair(File("<stdin>"), parseContent(parser, System.`in`.bufferedReader().readText()))
    }

private fun parseContent(
    parser: YamlParser,
    content: String,
): ParseResult = parser.parse(content)
