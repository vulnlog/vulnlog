// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.diagnosticSink
import dev.vulnlog.gradle.internal.parseInputOrFail
import dev.vulnlog.gradle.internal.requireNonEmptyVulnlogFiles
import dev.vulnlog.lib.core.FormatOutcome
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.checkFormat
import dev.vulnlog.lib.core.formatCommentsDroppedWarning
import dev.vulnlog.lib.core.formatFinding
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.formatYamlOutcome
import dev.vulnlog.lib.core.renderFormatFinding
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.hasYamlComments
import dev.vulnlog.lib.result.Severity
import dev.vulnlog.lib.shell.FileInputOption
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

    @TaskAction
    fun format() {
        val sink = diagnosticSink()
        val inputFiles = files.files.map { FileInputOption.File(it.toPath()) }
        requireNonEmptyVulnlogFiles(inputFiles)
        val parsed = parseInputOrFail(inputFiles, sink)

        val mapper = createYamlMapper()
        val checkOnly = check.getOrElse(false)
        val unformatted = mutableListOf<Path>()
        for (input in inputFiles) {
            val parsedInput = parsed.getValue(input)
            when (val outcome = formatYamlOutcome(parsedInput, mapper)) {
                is FormatOutcome.Unchanged ->
                    logger.lifecycle(formatStatus(StatusVerb.UNCHANGED, input.path.toString()))
                is FormatOutcome.Reformatted ->
                    if (checkOnly) {
                        unformatted.add(input.path)
                        val warning =
                            formatFinding(
                                Severity.WARNING,
                                input.path.toString(),
                                message = "not canonically formatted",
                            )
                        logger.warn(warning)
                        checkFormat(parsedInput, mapper).forEach { finding ->
                            logger.warn("  ${renderFormatFinding(finding)}")
                        }
                    } else {
                        if (hasYamlComments(parsedInput.rootNode)) {
                            logger.warn(formatCommentsDroppedWarning(input.path.toString()))
                        }
                        if (logger.isDebugEnabled) {
                            checkFormat(parsedInput, mapper).forEach { finding ->
                                sink.debug(renderFormatFinding(finding))
                            }
                        }
                        input.path.writeText(outcome.formatted.content)
                        sink.verbose("wrote ${input.path}")
                        logger.lifecycle(formatStatus(StatusVerb.FORMATTED, input.path.toString()))
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
}
