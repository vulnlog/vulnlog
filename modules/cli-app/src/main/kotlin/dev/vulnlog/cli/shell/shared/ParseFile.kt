// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell.shared

import dev.vulnlog.lib.parse.YamlParser
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.ParseResults
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.readText

fun parseStdin(inputStream: InputStream = System.`in`): ParseResults {
    val parser = YamlParser(createYamlMapper())

    val stdinFile = File("<stdin>")
    val content = inputStream.bufferedReader().readText()
    val fileToResult = mapOf(stdinFile to parser.parse(content))

    val ok = filterByType<ParseResult.Ok>(fileToResult)
    val failure = filterByType<ParseResult.Error>(fileToResult)
    return ParseResults(ok, failure)
}

fun parseFile(path: Path): ParseResults {
    val parser = YamlParser(createYamlMapper())

    val fileToResult = mapOf(path.toFile() to parser.parse(path.readText()))

    val ok = filterByType<ParseResult.Ok>(fileToResult)
    val failure = filterByType<ParseResult.Error>(fileToResult)
    return ParseResults(ok, failure)
}

fun parseFiles(paths: List<Path>): ParseResults {
    val parser = YamlParser(createYamlMapper())

    val fileToResult: Map<File, ParseResult> =
        paths.map(Path::toFile).associateWith { file -> parser.parse(file.readText()) }

    val ok = filterByType<ParseResult.Ok>(fileToResult)
    val failure = filterByType<ParseResult.Error>(fileToResult)
    return ParseResults(ok, failure)
}

private inline fun <reified T : ParseResult> filterByType(fileToResult: Map<File, ParseResult>): Map<File, T> =
    fileToResult
        .filterValues {
            it is T
        }.mapValues { (_, result) -> result as T }
