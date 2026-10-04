// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.failure
import dev.vulnlog.gradle.internal.log
import dev.vulnlog.gradle.internal.vulnlogFileInputs
import dev.vulnlog.gradle.internal.writeOrFail
import dev.vulnlog.gradle.validation.validateInputOrFail
import dev.vulnlog.lib.app.ChangelogFormatRequest
import dev.vulnlog.lib.app.ChangelogOutcome
import dev.vulnlog.lib.app.ChangelogRequest
import dev.vulnlog.lib.app.generateChangelog
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.model.reporting.ChangelogDetail
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.StatusVerb
import dev.vulnlog.lib.render.formatStatus
import dev.vulnlog.lib.render.renderChangelogMessages
import dev.vulnlog.lib.render.renderWritten
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
        val projects = vulnlogFileInputs(files.files).map { input -> validateInputOrFail(input).project }
        val request =
            ChangelogRequest(
                filter =
                    FilterRequest(
                        reporter = reporter.orNull,
                        asOf = asOf.orNull,
                        tags = tags.get(),
                        fixedIn = fixedIn.orNull,
                    ),
                format = ChangelogFormatRequest.fromToken(format.getOrElse("text")),
                detail = if (brief.getOrElse(false)) ChangelogDetail.BRIEF else ChangelogDetail.FULL,
            )

        val outcome = generateChangelog(projects, request)
        renderChangelogMessages(outcome).forEach(logger::log)
        when (outcome) {
            is ChangelogOutcome.Failed -> throw failure(outcome)
            // An empty changelog is written too, so a task consuming the declared output finds the file.
            is ChangelogOutcome.Generated -> write(outcome.content)
        }
    }

    private fun write(content: String) {
        val out = outputFile.get().asFile
        writeOrFail(writeOutput(out.toPath(), content, createDirectories = true))
        logger.log(renderWritten(out.path))
        logger.log(Message.Status(formatStatus(StatusVerb.WROTE, out.absolutePath)))
    }
}
