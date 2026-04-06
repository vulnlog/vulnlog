package dev.vulnlog.cli.shell.shared

import dev.vulnlog.cli.parse.YamlParser
import dev.vulnlog.cli.parse.createYamlMapper
import dev.vulnlog.cli.result.ParseResult
import dev.vulnlog.cli.result.ParseResults
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

fun merge(vararg results: ParseResults?): ParseResults {
    val all = results.filterNotNull()
    return ParseResults(
        success = all.flatMap { it.success.entries }.associate { it.key to it.value },
        failure = all.flatMap { it.failure.entries }.associate { it.key to it.value },
    )
}

private inline fun <reified T : ParseResult> filterByType(fileToResult: Map<File, ParseResult>): Map<File, T> {
    return fileToResult.filterValues { it is T }.mapValues { (_, result) -> result as T }
}
