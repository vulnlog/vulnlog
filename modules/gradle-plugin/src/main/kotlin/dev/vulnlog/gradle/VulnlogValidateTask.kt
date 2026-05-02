// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.parseInputOrFail
import dev.vulnlog.gradle.internal.requireNonEmptyVulnlogFiles
import dev.vulnlog.gradle.internal.validateParsedInputOrFailWithFailureOutput
import dev.vulnlog.lib.shell.FileInputOption
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class VulnlogValidateTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val files: ConfigurableFileCollection

    @get:Input
    abstract val strict: Property<Boolean>

    @TaskAction
    fun validate() {
        val inputFiles = files.files.map { FileInputOption.File(it.toPath()) }
        requireNonEmptyVulnlogFiles(inputFiles)
        val parsedSuccessfully = parseInputOrFail(inputFiles)
        val validationFindings = validateParsedInputOrFailWithFailureOutput(parsedSuccessfully)
        if (validationFindings.hasWarnings && strict.get()) {
            throw GradleException("Vulnlog validation failed.")
        }
        logger.lifecycle("Vulnlog validation OK")
    }
}
