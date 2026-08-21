// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.internal

import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.filter.renderFilterResolution
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.shell.DiagnosticSink
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.FilterValidationException
import dev.vulnlog.lib.shell.buildFilter
import org.gradle.api.GradleException
import java.io.File

/**
 * The Vulnlog files a task was configured with. The CLI gets the same guarantee from its argument
 * declaration, so both surfaces reject an empty selection before reading anything.
 */
fun vulnlogFileInputs(files: Iterable<File>): List<FileInputOption.File> {
    val inputFiles = files.map { FileInputOption.File(it.toPath()) }
    if (inputFiles.isEmpty()) {
        throw GradleException("No Vulnlog files configured. Set vulnlog.files in your build script.")
    }
    return inputFiles
}

/** The single Vulnlog file a task was configured with, for tasks that cannot merge several. */
fun singleVulnlogFileInput(
    taskName: String,
    files: Iterable<File>,
): FileInputOption.File {
    val inputFiles = vulnlogFileInputs(files)
    if (inputFiles.size > 1) {
        throw GradleException("$taskName supports a single Vulnlog file, but ${inputFiles.size} are configured.")
    }
    return inputFiles.single()
}

fun buildFilterOrFail(
    vulnlogFile: VulnlogFile,
    reporterOption: String?,
    releaseOption: Release?,
    tagsOptions: Set<Tag>,
    sink: DiagnosticSink = DiagnosticSink.NONE,
): ResolvedFilter =
    try {
        val filter = buildFilter(vulnlogFile, reporterOption, releaseOption, tagsOptions)
        renderFilterResolution(filter).forEach(sink::verbose)
        filter
    } catch (e: FilterValidationException) {
        throw GradleException("${e.message}. ${e.detail}")
    }
