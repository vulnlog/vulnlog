// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.diagnosticSink
import dev.vulnlog.gradle.internal.vulnlogFileInputs
import dev.vulnlog.gradle.validation.validateInputOrFail
import dev.vulnlog.lib.core.copyVulnerabilities
import dev.vulnlog.lib.core.findNonExistingVulnIds
import dev.vulnlog.lib.core.formatCommentsDroppedWarning
import dev.vulnlog.lib.core.formatCopiedMessage
import dev.vulnlog.lib.core.formatVulnIdsNotInSourceMessage
import dev.vulnlog.lib.core.parseVulnId
import dev.vulnlog.lib.parse.hasYamlComments
import dev.vulnlog.lib.parse.validation.ValidVulnlogProject
import dev.vulnlog.lib.shell.FileInputOption
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import kotlin.io.path.writeText

@DisableCachingByDefault(because = "Rewrites Vulnlog files in place")
abstract class VulnlogCopyTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFile: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val destinationFiles: ConfigurableFileCollection

    @get:Input
    abstract val vulnIds: SetProperty<String>

    @TaskAction
    fun generate() {
        val sink = diagnosticSink()
        val requestedIds = vulnIds.get().map { parseVulnId(it) }.toSet()
        val validatedSource =
            validateInputOrFail(FileInputOption.File(sourceFile.get().asFile.toPath())).project
        val sourceVulnlogFile = validatedSource.vulnlogProjectFile
        val missing = findNonExistingVulnIds(sourceVulnlogFile.vulnerabilities, requestedIds)
        if (missing.isNotEmpty()) {
            throw GradleException(formatVulnIdsNotInSourceMessage(missing))
        }

        val validatedDestinations: List<ValidVulnlogProject> =
            vulnlogFileInputs(destinationFiles.files).map { input ->
                validateInputOrFail(input).project
            }
        validatedDestinations.forEach { validDestination ->
            val outcome =
                copyVulnerabilities(
                    source = sourceVulnlogFile,
                    destination = validDestination,
                    vulnIds = requestedIds,
                )
            val source = validDestination.inputDocument.source
            if (hasYamlComments(validDestination.nodeTree.rootNode)) {
                logger.warn(formatCommentsDroppedWarning(source))
            }
            val destinationPath =
                requireNotNull(validDestination.inputDocument.path) { "Gradle inputs are always files" }
            destinationPath.writeText(outcome.newContent)
            sink.verbose("wrote $source")
            if (outcome.copied.isNotEmpty()) {
                sink.verbose("copied to $source: ${outcome.copied.joinToString(", ") { it.id }}")
            }
            logger.lifecycle(formatCopiedMessage(destinationPath, outcome.copied))
        }
    }
}
