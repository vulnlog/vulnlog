// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.filter.resolveFilterOrFail
import dev.vulnlog.gradle.internal.diagnosticSink
import dev.vulnlog.gradle.internal.vulnlogFileInputs
import dev.vulnlog.gradle.reporting.sharedProjectOrFail
import dev.vulnlog.gradle.validation.validateInputOrFail
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.applyFilter
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.reporting.collectChangelogReleases
import dev.vulnlog.lib.core.reporting.formatChangelogMarkdown
import dev.vulnlog.lib.core.reporting.formatChangelogText
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.model.reporting.ChangelogDetail
import dev.vulnlog.lib.model.reporting.ReportingChangelogProject
import dev.vulnlog.lib.model.reporting.ReportingChangelogRelease
import dev.vulnlog.lib.parse.validation.ValidVulnlogProject
import dev.vulnlog.lib.shell.ChangelogFormatRequest
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

@CacheableTask
abstract class VulnlogChangelogReportTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val files: ConfigurableFileCollection

    @get:Input
    @get:Optional
    abstract val format: Property<String>

    @get:Input
    abstract val brief: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val fixedIn: Property<String>

    @get:Input
    @get:Optional
    abstract val reporter: Property<String>

    @get:Input
    @get:Optional
    abstract val asOf: Property<String>

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

        val request =
            FilterRequest(
                reporter = reporter.orNull,
                asOf = asOf.orNull,
                tags = tags.get(),
                fixedIn = fixedIn.orNull,
            )
        val filter = resolveFilterOrFail(request, vulnlogFiles)

        val releases: List<ReportingChangelogRelease> =
            collectChangelogReleases(vulnlogFiles.map { it.applyFilter(filter) })
        sink.debug("collected ${releases.sumOf { it.entries.size }} fixes in ${releases.size} releases")
        if (releases.isEmpty()) {
            logger.lifecycle(formatMessage(FindingSeverity.INFO, "no fixed vulnerabilities to report"))
        }

        val detail = if (brief.getOrElse(false)) ChangelogDetail.BRIEF else ChangelogDetail.FULL
        val changelog = ReportingChangelogProject(project, releases)
        val content =
            when (ChangelogFormatRequest.fromToken(format.getOrElse("text"))) {
                is ChangelogFormatRequest.Text -> formatChangelogText(changelog, detail)
                is ChangelogFormatRequest.Markdown -> formatChangelogMarkdown(changelog, detail)
            }

        val out = outputFile.get().asFile
        out.parentFile?.mkdirs()
        out.writeText(content)
        sink.verbose("wrote ${out.path}")
        logger.lifecycle(formatStatus(StatusVerb.WROTE, out.absolutePath))
    }
}
