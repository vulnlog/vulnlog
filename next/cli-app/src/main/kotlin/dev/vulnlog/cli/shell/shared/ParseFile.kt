package dev.vulnlog.cli.shell.shared

import dev.vulnlog.cli.parse.YamlParser
import dev.vulnlog.cli.parse.createYamlMapper
import dev.vulnlog.cli.result.ParseResult
import dev.vulnlog.cli.result.ParseResults
import java.io.File
import java.nio.file.Path

fun parseFiles(paths: List<Path>): ParseResults {
    val parser = YamlParser(createYamlMapper())

    val fileToResult: Map<File, ParseResult> =
        paths.map(Path::toFile).associateWith { file -> parser.parse(file.readText()) }

    val ok = filterByType<ParseResult.Ok>(fileToResult)
    val failure = filterByType<ParseResult.Error>(fileToResult)
    return ParseResults(ok, failure)
}

private inline fun <reified T : ParseResult> filterByType(fileToResult: Map<File, ParseResult>): Map<File, T> {
    return fileToResult.filterValues { it is T }.mapValues { (_, result) -> result as T }
}
