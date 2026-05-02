// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.parseInputOrFail
import dev.vulnlog.gradle.internal.validateParsedInputOrFailWithFailureOutput
import dev.vulnlog.lib.core.findNonExistingVulnIds
import dev.vulnlog.lib.core.insertEntryAfterVulnerabilitiesHeader
import dev.vulnlog.lib.core.latestPublishedRelease
import dev.vulnlog.lib.core.parseVulnId
import dev.vulnlog.lib.core.serializeEntryYaml
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.v1.V1Mapper
import dev.vulnlog.lib.shell.FileInputOption
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import kotlin.io.path.readText
import kotlin.io.path.writeText

@CacheableTask
abstract class VulnlogCopyTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:OutputFiles
    abstract val destinationFiles: ConfigurableFileCollection

    @get:Input
    abstract val vulnIds: SetProperty<String>

    @TaskAction
    fun generate() {
        val inputFiles = sourceFiles.files.map { FileInputOption.File(it.toPath()) }
        if (inputFiles.isEmpty()) {
            throw GradleException("No Vulnlog files specified.")
        }
        if (inputFiles.size > 1) {
            throw GradleException("Copy task supports only a single Vulnlog file.")
        }

        val parsedSuccessfully = parseInputOrFail(inputFiles)
        validateParsedInputOrFailWithFailureOutput(parsedSuccessfully)

        val sourceVulnlogFile = parsedSuccessfully.values.first().content
        val vulnIds = vulnIds.get().map { parseVulnId(it) }.toSet()
        val vulnIdsNotInSourceContent = findNonExistingVulnIds(sourceVulnlogFile.vulnerabilities, vulnIds)
        if (vulnIdsNotInSourceContent.isNotEmpty()) {
            throw GradleException(
                "Error: Vulnerability IDs not found in source file: ${
                    vulnIdsNotInSourceContent.joinToString(
                        ", ",
                    )
                }",
            )
        }

        val mapper = createYamlMapper()

        val destinations = destinationFiles.files.map { FileInputOption.File(it.toPath()) }
        for (destination in destinations) {
            val parsedTargetSuccessfully = parseInputOrFail(listOf(destination))
            validateParsedInputOrFailWithFailureOutput(parsedTargetSuccessfully)

            val destinationVulnlogFile = parsedTargetSuccessfully.values.first().content
            val vulnIdsNotInTargetContent = findNonExistingVulnIds(destinationVulnlogFile.vulnerabilities, vulnIds)
            val vulnIdsToIgnore = vulnIds.minus(vulnIdsNotInTargetContent)
            if (vulnIdsToIgnore.isNotEmpty()) {
                logger.warn(
                    "Warning: Skipping IDs already exist in ${destination.path}: ${
                        vulnIdsToIgnore.joinToString(
                            ", ",
                        )
                    }",
                )
            }
            val latestRelease = latestPublishedRelease(destinationVulnlogFile.releases)

            val vulnEntries =
                sourceVulnlogFile.vulnerabilities
                    .filter { it.id in vulnIdsNotInTargetContent }
                    // to maintain order when inserting entries as the first one in insertEntryAfterVulnerabilitiesHeader
                    .reversed()

            var destinationContent = destination.path.readText()
            for (entry in vulnEntries) {
                val dto = V1Mapper.vulnerabilityToDto(entry)
                val adjustedDto = dto.copy(releases = listOf(latestRelease.value))
                val entryYaml = serializeEntryYaml(adjustedDto, mapper)
                destinationContent = insertEntryAfterVulnerabilitiesHeader(destinationContent, entryYaml)
            }
            destination.path.writeText(destinationContent)
            logger.lifecycle("Copied to ${destination.path}: ${vulnIdsNotInTargetContent.joinToString(", ") { it.id }}")
        }
    }
}
