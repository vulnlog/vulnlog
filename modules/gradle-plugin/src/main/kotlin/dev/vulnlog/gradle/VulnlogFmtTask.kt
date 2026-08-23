// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.diagnosticSink
import dev.vulnlog.gradle.internal.vulnlogFileInputs
import dev.vulnlog.gradle.validation.parseInputOrFail
import dev.vulnlog.lib.core.FormatOutcome
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.checkFormat
import dev.vulnlog.lib.core.formatCommentsDroppedWarning
import dev.vulnlog.lib.core.formatFinding
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.formatYamlOutcome
import dev.vulnlog.lib.core.renderFormatFinding
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.parse.hasYamlComments
import dev.vulnlog.lib.parse.validation.ParsedVulnlogProject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
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
import java.nio.file.Path
import kotlin.io.path.writeText

@DisableCachingByDefault(because = "Rewrites Vulnlog files in place")
abstract class VulnlogFmtTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val files: ConfigurableFileCollection

    @get:Input
    @get:Optional
    @get:Option(option = "check", description = "Do not write changes; fail if any file is not already formatted.")
    abstract val check: Property<Boolean>

    /**
     * Formatting rewrites the document as written, so it stops after the DTO stage: a file whose
     * domain rules do not hold is still formattable, and often needs formatting to be readable.
     */
    @TaskAction
    fun format() {
        val inputFiles = vulnlogFileInputs(files.files)
        val parsed: List<ParsedVulnlogProject> =
            inputFiles.map { input -> parseInputOrFail(input).project }

        val checkOnly = check.getOrElse(false)
        val unformatted = mutableListOf<Path>()
        for (parsedInput in parsed) {
            val source = parsedInput.inputDocument.source
            when (val outcome = formatYamlOutcome(parsedInput)) {
                is FormatOutcome.Unchanged ->
                    logger.lifecycle(formatStatus(StatusVerb.UNCHANGED, source))

                is FormatOutcome.Reformatted ->
                    if (checkOnly) {
                        unformatted.add(inputPathOf(parsedInput))
                        logFormatCheckFindings(parsedInput, source)
                    } else {
                        writeReformatted(parsedInput, source, outcome.formatted)
                    }
            }
        }
        if (checkOnly && unformatted.isNotEmpty()) {
            throw GradleException(
                "Some Vulnlog files are not formatted: ${unformatted.joinToString(", ")}. " +
                    "Run the vulnlogFormat task to fix them.",
            )
        }
    }

    private fun writeReformatted(
        parsedInput: ParsedVulnlogProject,
        source: String,
        formatted: String,
    ) {
        if (hasYamlComments(parsedInput.nodeTree.rootNode)) {
            logger.warn(formatCommentsDroppedWarning(source))
        }
        debugFormatFindings(parsedInput)
        inputPathOf(parsedInput).writeText(formatted)
        diagnosticSink().verbose("wrote $source")
        logger.lifecycle(formatStatus(StatusVerb.FORMATTED, source))
    }

    private fun logFormatCheckFindings(
        parsedInput: ParsedVulnlogProject,
        source: String,
    ) {
        logger.warn(formatFinding(FindingSeverity.WARNING, source, message = "not canonically formatted"))
        checkFormat(parsedInput).forEach { finding ->
            logger.warn("  ${renderFormatFinding(finding)}")
        }
    }

    private fun debugFormatFindings(parsedInput: ParsedVulnlogProject) {
        if (!logger.isDebugEnabled) return
        checkFormat(parsedInput).forEach { finding ->
            diagnosticSink().debug(renderFormatFinding(finding))
        }
    }

    /** Gradle inputs are always real files, unlike the CLI which also accepts STDIN. */
    private fun inputPathOf(parsedInput: ParsedVulnlogProject): Path =
        requireNotNull(parsedInput.inputDocument.path) { "Gradle inputs are always files" }
}
