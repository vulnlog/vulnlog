// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatStatus
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** The only task writing into the source tree, so that [VulnlogOpenVexTask] stays cacheable. */
@DisableCachingByDefault(because = "Copies one file into the source tree")
abstract class VulnlogOpenVexUpdateTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val generatedFile: RegularFileProperty

    @get:OutputFile
    abstract val baseline: RegularFileProperty

    @TaskAction
    fun update() {
        val source = generatedFile.get().asFile
        val target = baseline.get().asFile
        val content = source.readBytes()
        if (target.isFile && target.readBytes().contentEquals(content)) {
            logger.lifecycle(formatStatus(StatusVerb.UNCHANGED, target.absolutePath))
            return
        }
        target.parentFile?.mkdirs()
        target.writeBytes(content)
        logger.lifecycle(formatStatus(StatusVerb.WROTE, target.absolutePath))
    }
}
