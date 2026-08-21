// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.buildFilterOrFail
import dev.vulnlog.gradle.internal.diagnosticSink
import dev.vulnlog.gradle.internal.singleVulnlogFileInput
import dev.vulnlog.gradle.validation.validateInputOrFail
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.SuppressionFilter
import dev.vulnlog.lib.core.buildSuppressionOutputs
import dev.vulnlog.lib.core.collectSuppressedVulnerabilities
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.renderSuppressionExclusion
import dev.vulnlog.lib.core.renderSuppressionInclusions
import dev.vulnlog.lib.core.renderSuppressionWritten
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.parse.suppression.SuppressionWriter
import dev.vulnlog.lib.shell.SuppressionFormatRequest
import org.gradle.api.DefaultTask
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

    @get:Input
    @get:Optional
    abstract val format: Property<String>

    @TaskAction
    fun generate() {
        val sink = diagnosticSink()
        val inputFile = singleVulnlogFileInput(name, files.files)
        val validated = validateInputOrFail(inputFile).project

        val vulnlogFile = validated.vulnlogProjectFile
        val filter =
            buildFilterOrFail(
                vulnlogFile,
                reporter.orNull,
                release.orNull?.let(::Release),
                tags.get().map(::Tag).toSet(),
                sink,
            )

        val targetReporters =
            vulnlogFile.vulnerabilities
                .flatMap { it.reports }
                .map { it.reporter }
                .filter { filter.reporter == null || it == filter.reporter }
                .toSet()

        val collected = collectSuppressedVulnerabilities(vulnlogFile, SuppressionFilter(filter))
        val suppressionFormatRequest: SuppressionFormatRequest =
            SuppressionFormatRequest.fromToken(
                format.getOrElse("auto"),
            )
        val suppressionResult = buildSuppressionOutputs(targetReporters, collected.included, suppressionFormatRequest)
        (collected.exclusions + suppressionResult.exclusions).forEach { exclusion ->
            sink.verbose(renderSuppressionExclusion(exclusion))
        }
        renderSuppressionInclusions(collected.included).forEach(sink::debug)
        val outputs = suppressionResult.outputs
        if (outputs.isEmpty()) {
            logger.lifecycle(formatStatus(StatusVerb.UNCHANGED, "no suppression entries applicable"))
            return
        }

        val dir = outputDir.get().asFile
        dir.mkdirs()
        outputs.forEach { suppressionOutput ->
            val suppressionFile = SuppressionWriter.writeSuppressionOutput(suppressionOutput)
            val outputPath = dir.resolve(suppressionFile.fileName)
            outputPath.writeText(suppressionFile.content)
            logger.lifecycle(formatStatus(StatusVerb.WROTE, outputPath.absolutePath))
            sink.verbose(renderSuppressionWritten(outputPath.path, suppressionOutput))
        }
    }
}
