// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.internal

import dev.vulnlog.lib.io.FileInputOption
import dev.vulnlog.lib.model.OutputWrite
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

/** Returns the write when it succeeded, so a caller can tell [OutputWrite.Unchanged] apart. */
fun writeOrFail(write: OutputWrite): OutputWrite = if (write is OutputWrite.Failed) throw failure(write) else write
