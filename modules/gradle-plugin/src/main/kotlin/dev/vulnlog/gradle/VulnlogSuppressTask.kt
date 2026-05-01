// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.buildFilter
import dev.vulnlog.gradle.internal.parseInputOrFail
import dev.vulnlog.gradle.internal.validateParsedInputOrFailWithFailureOutput
import dev.vulnlog.lib.core.SuppressionFilter
import dev.vulnlog.lib.core.collectSuppressedVulnerabilities
import dev.vulnlog.lib.core.mapToSuppression
import dev.vulnlog.lib.parse.suppression.SuppressionWriter
import dev.vulnlog.lib.shell.FileInputOption
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class VulnlogSuppressTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val files: ConfigurableFileCollection

    @get:Input
    @get:Optional
    abstract val reporter: Property<String>

    @get:Input
    @get:Optional
    abstract val release: Property<String>

    @get:Input
    abstract val tags: SetProperty<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val inputFiles = files.files.map { FileInputOption.File(it.toPath()) }
        if (inputFiles.isEmpty()) {
            throw GradleException("No Vulnlog files configured. Set vulnlog.files in your build script.")
        }
        if (inputFiles.size > 1) {
            throw GradleException(
                "vulnlogSuppress supports a single Vulnlog file, but ${inputFiles.size} are configured.",
            )
        }
        val parsedSuccessfully = parseInputOrFail(inputFiles)
        validateParsedInputOrFailWithFailureOutput(parsedSuccessfully)

        val vulnlogFile = parsedSuccessfully.values.first().content
        val filter = buildFilter(vulnlogFile, reporter.orNull, release.orNull, tags.get())

        val targetReporters =
            vulnlogFile.vulnerabilities
                .flatMap { it.reports }
                .map { it.reporter }
                .filter { filter.reporter == null || it == filter.reporter }
                .toSet()

        val suppressionVulns = collectSuppressedVulnerabilities(vulnlogFile, SuppressionFilter(filter))
        val outputs = mapToSuppression(targetReporters, suppressionVulns)

        val dir = outputDir.get().asFile
        dir.mkdirs()
        outputs.forEach { suppressionOutput ->
            val suppressionFile = SuppressionWriter.writeSuppressionOutput(suppressionOutput)
            val outputPath = dir.resolve(suppressionFile.fileName)
            outputPath.writeText(suppressionFile.content)
            logger.lifecycle("Suppression file created at: ${outputPath.absolutePath}")
        }
    }
}
