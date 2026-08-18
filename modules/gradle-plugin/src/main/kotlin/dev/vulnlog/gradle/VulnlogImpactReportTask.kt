// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.filter.resolveFilterOrFail
import dev.vulnlog.gradle.internal.diagnosticSink
import dev.vulnlog.gradle.internal.vulnlogFileInputs
import dev.vulnlog.gradle.reporting.sharedProjectOrFail
import dev.vulnlog.gradle.validation.validateInputOrFail
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.applyFilter
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.reporting.collectReportingEntries
import dev.vulnlog.lib.core.reporting.mergeReportingEntries
import dev.vulnlog.lib.core.reporting.renderReportingCounts
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.report.ReportingEntry
import dev.vulnlog.lib.parse.reporting.HtmlReportMapper
import dev.vulnlog.lib.parse.reporting.HtmlReportWriter
import dev.vulnlog.lib.parse.reporting.dto.FilterDataDto
import dev.vulnlog.lib.parse.validation.ValidVulnlogProject
import org.gradle.api.DefaultTask
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
import java.time.Instant

@CacheableTask
abstract class VulnlogImpactReportTask : DefaultTask() {
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
        val sink = diagnosticSink()

        val validated: List<ValidVulnlogProject> =
            vulnlogFileInputs(files.files).map { input -> validateInputOrFail(input).project }
        val vulnlogFiles: List<VulnlogFile> = validated.map(ValidVulnlogProject::vulnlogProjectFile)
        val project = sharedProjectOrFail(vulnlogFiles)

        val request = FilterRequest(reporter.orNull, release.orNull, tags.get())
        val filter = resolveFilterOrFail(request, vulnlogFiles)

        val reported: List<ReportingEntry> = vulnlogFiles.flatMap { collectReportingEntries(it.applyFilter(filter)) }
        val merged = mergeReportingEntries(reported)
        sink.debug(renderReportingCounts(reported.size, merged.size))

        val filterData =
            FilterDataDto(
                release = release.orNull,
                tags = filter.tags.map(Tag::value).sorted(),
                reporter = filter.reporter?.canonical(),
            )
        val inputNames = validated.map { it.inputDocument.filename }

        val reportData =
            HtmlReportMapper.toDto(
                project = project,
                entries = merged,
                generatedAt = Instant.now(),
                vulnlogVersion = BuildInfo.VERSION,
                inputs = inputNames,
                filter = filterData,
            )
        val reportContent = HtmlReportWriter.renderHtmlReport(reportData)

        val out = outputFile.get().asFile
        out.parentFile?.mkdirs()
        out.writeText(reportContent)
        sink.verbose("wrote ${out.path}")
        logger.lifecycle(formatStatus(StatusVerb.WROTE, out.absolutePath))
    }
}
