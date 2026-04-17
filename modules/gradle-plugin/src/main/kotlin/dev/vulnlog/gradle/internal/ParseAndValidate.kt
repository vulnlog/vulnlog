// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.gradle.internal

import dev.vulnlog.lib.core.renderValidation
import dev.vulnlog.lib.core.validate
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.VulnlogFileContext
import dev.vulnlog.lib.parse.YamlParser
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.result.ParseResult
import org.gradle.api.GradleException
import java.io.File

internal fun parseAndValidate(files: Collection<File>): List<VulnlogFile> {
    val parser = YamlParser(createYamlMapper())
    val parseResults = files.associateWith { file -> parser.parse(file.readText()) }

    val errors = parseResults.filter { (_, result) -> result is ParseResult.Error }
    if (errors.isNotEmpty()) {
        val messages =
            errors.map { (file, result) ->
                "Parsing of ${file.name} failed:\n${(result as ParseResult.Error).error}"
            }
        throw GradleException(messages.joinToString("\n\n"))
    }

    val ok = parseResults.mapValues { (_, result) -> result as ParseResult.Ok }

    val findings =
        ok.map { (file, result) ->
            val context = VulnlogFileContext(result.validationVersion, file.name, result.content)
            context to validate(context)
        }

    val rendered =
        findings
            .filter { (_, r) -> r.errors.isNotEmpty() }
            .joinToString("\n\n") { (ctx, r) ->
                "Validation findings for ${ctx.fileName}:\n${renderValidation(r)}"
            }
    if (rendered.isNotBlank()) {
        throw GradleException(rendered)
    }

    return ok.values.map { it.content }
}
