// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.parseInputOrFail
import dev.vulnlog.gradle.internal.validateParsedInputOrFailWithFailureOutput
import dev.vulnlog.lib.core.copyVulnerabilities
import dev.vulnlog.lib.core.findNonExistingVulnIds
import dev.vulnlog.lib.core.formatCopiedMessage
import dev.vulnlog.lib.core.formatSkippedExistingMessage
import dev.vulnlog.lib.core.formatVulnIdsNotInSourceMessage
import dev.vulnlog.lib.core.parseVulnId
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.shell.FileInputOption
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import kotlin.io.path.readText
import kotlin.io.path.writeText

abstract class VulnlogCopyTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFile: RegularFileProperty

    @get:InputFiles
    @get:OutputFiles
    abstract val destinationFiles: ConfigurableFileCollection

    @get:Input
    abstract val vulnIds: SetProperty<String>

    @TaskAction
    fun generate() {
        val sourceInput = FileInputOption.File(sourceFile.get().asFile.toPath())
        val parsedSource = parseInputOrFail(listOf(sourceInput))
        validateParsedInputOrFailWithFailureOutput(parsedSource)
        val sourceVulnlogFile = parsedSource.values.first().content

        val vulnIdSet = vulnIds.get().map { parseVulnId(it) }.toSet()
        val missing = findNonExistingVulnIds(sourceVulnlogFile.vulnerabilities, vulnIdSet)
        if (missing.isNotEmpty()) {
            throw GradleException(formatVulnIdsNotInSourceMessage(missing))
        }

        val mapper = createYamlMapper()
        val destinations = destinationFiles.files.map { FileInputOption.File(it.toPath()) }
        for (destination in destinations) {
            val parsedDestination = parseInputOrFail(listOf(destination))
            validateParsedInputOrFailWithFailureOutput(parsedDestination)
            val destinationVulnlogFile = parsedDestination.values.first().content

            val outcome =
                copyVulnerabilities(
                    source = sourceVulnlogFile,
                    destination = destinationVulnlogFile,
                    destinationContent = destination.path.readText(),
                    vulnIds = vulnIdSet,
                    mapper = mapper,
                )
            if (outcome.skippedAlreadyExisting.isNotEmpty()) {
                logger.warn(formatSkippedExistingMessage(destination.path, outcome.skippedAlreadyExisting))
            }
            destination.path.writeText(outcome.newContent)
            logger.lifecycle(formatCopiedMessage(destination.path, outcome.copied))
        }
    }
}
