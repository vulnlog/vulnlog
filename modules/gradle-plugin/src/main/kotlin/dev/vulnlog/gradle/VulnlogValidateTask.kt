// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.gradle

import dev.vulnlog.lib.core.renderValidation
import dev.vulnlog.lib.core.validate
import dev.vulnlog.lib.model.VulnlogFileContext
import dev.vulnlog.lib.parse.YamlParser
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.result.ParseResult
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class VulnlogValidateTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val files: ConfigurableFileCollection

    @get:Input
    abstract val strict: Property<Boolean>

    @TaskAction
    fun validate() {
        val inputFiles = files.files
        if (inputFiles.isEmpty()) {
            throw GradleException("No Vulnlog files configured. Set vulnlog.files in your build script.")
        }

        val parser = YamlParser(createYamlMapper())
        val parseResults = inputFiles.associateWith { file -> parser.parse(file.readText()) }

        val errors = parseResults.filter { (_, result) -> result is ParseResult.Error }
        if (errors.isNotEmpty()) {
            val messages =
                errors.map { (file, result) ->
                    "Parsing of ${file.name} failed:\n${(result as ParseResult.Error).error}"
                }
            throw GradleException(messages.joinToString("\n\n"))
        }

        val successResults = parseResults.mapValues { (_, result) -> result as ParseResult.Ok }

        val contextToResults =
            successResults.map { (file, parseResult) ->
                val context = VulnlogFileContext(parseResult.validationVersion, file.name, parseResult.content)
                context to validate(context)
            }

        val renderedFindings =
            contextToResults
                .filter { (_, result) -> result.findings.isNotEmpty() }
                .joinToString("\n\n") { (context, result) ->
                    "Validation findings for ${context.fileName}:\n${renderValidation(result)}"
                }

        val hasErrors = contextToResults.any { (_, result) -> result.errors.isNotEmpty() }
        val hasWarnings = contextToResults.any { (_, result) -> result.warnings.isNotEmpty() }

        if (renderedFindings.isNotBlank()) {
            logger.warn(renderedFindings)
        }

        if (hasErrors || (hasWarnings && strict.get())) {
            throw GradleException("Vulnlog validation failed.")
        } else {
            logger.lifecycle("Vulnlog validation OK")
        }
    }
}
