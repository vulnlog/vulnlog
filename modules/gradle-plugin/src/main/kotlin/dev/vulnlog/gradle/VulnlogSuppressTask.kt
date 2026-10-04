// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.failure
import dev.vulnlog.gradle.internal.log
import dev.vulnlog.gradle.internal.singleVulnlogFileInput
import dev.vulnlog.gradle.internal.writeOrFail
import dev.vulnlog.gradle.validation.validateInputOrFail
import dev.vulnlog.lib.app.SuppressionFile
import dev.vulnlog.lib.app.SuppressionFormatRequest
import dev.vulnlog.lib.app.SuppressionOutcome
import dev.vulnlog.lib.app.SuppressionRequest
import dev.vulnlog.lib.app.generateSuppressions
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.StatusVerb
import dev.vulnlog.lib.render.formatStatus
import dev.vulnlog.lib.render.renderSuppressionMessages
import dev.vulnlog.lib.render.renderSuppressionWritten
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
import java.time.LocalDate

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
    abstract val asOf: Property<String>

    @get:Input
    abstract val tags: SetProperty<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val format: Property<String>

    @TaskAction
    fun generate() {
        val inputFile = singleVulnlogFileInput(name, files.files)
        val project = validateInputOrFail(inputFile).project
        val request =
            SuppressionRequest(
                filter = FilterRequest(reporter = reporter.orNull, asOf = asOf.orNull, tags = tags.get()),
                format = SuppressionFormatRequest.fromToken(format.getOrElse("auto")),
                today = LocalDate.now(),
                singleFile = false,
            )

        val outcome = generateSuppressions(project, request)
        renderSuppressionMessages(outcome).forEach(logger::log)
        when (outcome) {
            is SuppressionOutcome.Failed -> throw failure(outcome)
            is SuppressionOutcome.NothingToSuppress -> Unit
            is SuppressionOutcome.Generated -> outcome.files.forEach(::write)
        }
    }

    private fun write(file: SuppressionFile) {
        val out = outputDir.get().asFile.resolve(file.fileName)
        writeOrFail(writeOutput(out.toPath(), file.content, createDirectories = true))
        logger.log(Message.Status(formatStatus(StatusVerb.WROTE, out.absolutePath)))
        logger.log(renderSuppressionWritten(out.path, file))
    }
}
