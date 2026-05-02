// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.buildFilterOrFail
import dev.vulnlog.gradle.internal.parseInputOrFail
import dev.vulnlog.gradle.internal.validateParsedInputOrFailWithFailureOutput
import dev.vulnlog.lib.core.collectReportingEntries
import dev.vulnlog.lib.core.mergeReportingEntries
import dev.vulnlog.lib.core.validateSharedProject
import dev.vulnlog.lib.parse.reporting.HtmlReportMapper
import dev.vulnlog.lib.parse.reporting.HtmlReportWriter
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.shell.FileInputOption
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.time.LocalDate

@CacheableTask
abstract class VulnlogReportTask : DefaultTask() {
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

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val inputFiles = files.files.map { FileInputOption.File(it.toPath()) }
        if (inputFiles.isEmpty()) {
            throw GradleException("No Vulnlog files configured. Set vulnlog.files in your build script.")
        }
        val parsedSuccessfully = parseInputOrFail(inputFiles)
        validateParsedInputOrFailWithFailureOutput(parsedSuccessfully)

        val vulnlogFiles = parsedSuccessfully.values.map(ParseResult.Ok::content)

        val project =
            validateSharedProject(vulnlogFiles)
                ?: throw GradleException("All input files must share the same project metadata.")

        val filter = buildFilterOrFail(vulnlogFiles.first(), reporter.orNull, release.orNull, tags.get())

        val allEntries = vulnlogFiles.flatMap { collectReportingEntries(it, filter) }
        val merged = mergeReportingEntries(allEntries)

        val reportData = HtmlReportMapper.toDto(project, merged, LocalDate.now())
        val reportContent = HtmlReportWriter.renderHtmlReport(reportData)

        val out = outputFile.get().asFile
        out.parentFile?.mkdirs()
        out.writeText(reportContent)
        logger.lifecycle("Report written to: ${out.absolutePath}")
    }
}
