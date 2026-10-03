// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.failure
import dev.vulnlog.gradle.internal.log
import dev.vulnlog.gradle.internal.vulnlogFileInputs
import dev.vulnlog.gradle.internal.writeOrFail
import dev.vulnlog.gradle.validation.validateInputOrFail
import dev.vulnlog.lib.app.ImpactReportOutcome
import dev.vulnlog.lib.app.ImpactReportRequest
import dev.vulnlog.lib.app.generateImpactReport
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.renderImpactReportMessages
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
    abstract val asOf: Property<String>

    @get:Input
    abstract val tags: SetProperty<String>

    @get:Input
    abstract val states: SetProperty<String>

    @get:Input
    abstract val verdicts: SetProperty<String>

    @get:Input
    abstract val dispositions: SetProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val projects = vulnlogFileInputs(files.files).map { input -> validateInputOrFail(input).project }
        val request =
            ImpactReportRequest(
                filter =
                    FilterRequest(
                        reporter = reporter.orNull,
                        asOf = asOf.orNull,
                        tags = tags.get(),
                        states = states.get(),
                        verdicts = verdicts.get(),
                        dispositions = dispositions.get(),
                    ),
                generatedAt = Instant.now(),
                vulnlogVersion = BuildInfo.VERSION,
            )

        val outcome = generateImpactReport(projects, request)
        renderImpactReportMessages(outcome).forEach(logger::log)
        when (outcome) {
            is ImpactReportOutcome.Failed -> throw failure(outcome)
            is ImpactReportOutcome.Generated -> write(outcome.content)
        }
    }

    private fun write(content: String) {
        val out = outputFile.get().asFile
        writeOrFail(writeOutput(out.toPath(), content, createDirectories = true))
        logger.log(renderWritten(out.path))
        logger.log(Message.Status(formatStatus(StatusVerb.WROTE, out.absolutePath)))
    }
}
