// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.parseInputOrFail
import dev.vulnlog.gradle.internal.requireNonEmptyVulnlogFiles
import dev.vulnlog.gradle.internal.validateParsedInputOrFailWithFailureOutput
import dev.vulnlog.lib.core.formatYaml
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.sourceFile
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
        val inputFiles = files.files.map { FileInputOption.File(it.toPath()) }
        requireNonEmptyVulnlogFiles(inputFiles)
        // Only format files that parse and validate without errors.
        val parsed = parseInputOrFail(inputFiles)
        validateParsedInputOrFailWithFailureOutput(parsed)

        val mapper = createYamlMapper()
        val checkOnly = check.getOrElse(false)
        val unformatted = mutableListOf<Path>()
        for (input in inputFiles) {
            val raw = parsed.getValue(input.sourceFile()).rawContent
            val formatted = formatYaml(raw, mapper)
            when {
                formatted == raw -> logger.lifecycle("Already formatted: ${input.path}")
                checkOnly -> {
                    unformatted.add(input.path)
                    logger.lifecycle("Can be reformatted: ${input.path}")
                }
                else -> {
                    input.path.writeText(formatted)
                    logger.lifecycle("Formatted: ${input.path}")
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
