// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.failure
import dev.vulnlog.gradle.internal.log
import dev.vulnlog.gradle.internal.vulnlogFileInputs
import dev.vulnlog.gradle.internal.writeOrFail
import dev.vulnlog.gradle.validation.parseInputOrFail
import dev.vulnlog.lib.app.FormatOutcome
import dev.vulnlog.lib.app.FormatRequest
import dev.vulnlog.lib.app.formatDocument
import dev.vulnlog.lib.document.InputDocument
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.StatusVerb
import dev.vulnlog.lib.render.formatStatus
import dev.vulnlog.lib.render.renderFormatMessages
import dev.vulnlog.lib.render.renderWritten
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Rewrites Vulnlog files in place")
abstract class VulnlogFmtTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val files: ConfigurableFileCollection

    @get:Input
    @get:Optional
    @get:Option(option = "check", description = "Do not write changes; fail if any file is not already formatted.")
    abstract val check: Property<Boolean>

    @TaskAction
    fun format() {
        val projects = vulnlogFileInputs(files.files).map { input -> parseInputOrFail(input).project }
        val request = FormatRequest(check = check.getOrElse(false))

        val outcomes = projects.map { project -> formatDocument(project, request) }
        outcomes.forEach { outcome ->
            renderFormatMessages(outcome).forEach(logger::log)
            when (outcome) {
                is FormatOutcome.Unchanged ->
                    logger.log(Message.Status(formatStatus(StatusVerb.UNCHANGED, outcome.document.source)))

                is FormatOutcome.Reformatted -> write(outcome.document, outcome.formatted)

                is FormatOutcome.NotCanonical -> Unit
            }
        }

        val notCanonical = outcomes.filterIsInstance<FormatOutcome.NotCanonical>()
        if (notCanonical.isNotEmpty()) throw failure(notCanonical)
    }

    private fun write(
        document: InputDocument,
        formatted: String,
    ) {
        val path = requireNotNull(document.path) { "Gradle inputs are always files" }
        writeOrFail(writeOutput(path, formatted))
        logger.log(renderWritten(document.source))
        logger.log(Message.Status(formatStatus(StatusVerb.FORMATTED, document.source)))
    }
}
