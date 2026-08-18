// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.diagnosticSink
import dev.vulnlog.gradle.internal.vulnlogFileInputs
import dev.vulnlog.gradle.validation.validateInputOrFail
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.validation.ValidationConfig
import dev.vulnlog.lib.core.validation.ValidationOutcome
import dev.vulnlog.lib.core.validation.renderValidationSummary
import dev.vulnlog.lib.model.finding.ALL_SEVERITIES
import dev.vulnlog.lib.parse.validation.ValidVulnlogProject
import dev.vulnlog.lib.shell.ValidationRequest
import org.gradle.api.DefaultTask
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
        val request = ValidationRequest(ValidationConfig(strict.get()), ALL_SEVERITIES)
        vulnlogFileInputs(files.files)
            .map { input -> validateInputOrFail(input, request) }
            .forEach(::printSummary)
    }

    private fun printSummary(validated: ValidationOutcome.Ok<ValidVulnlogProject>) {
        val filename = validated.project.inputDocument.filename
        diagnosticSink().verbose(renderValidationSummary(filename, validated.findings))
        logger.lifecycle(formatStatus(StatusVerb.VALIDATED, filename))
    }
}
