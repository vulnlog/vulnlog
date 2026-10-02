// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.log
import dev.vulnlog.gradle.internal.writeOrFail
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.io.writeOutputIfChanged
import dev.vulnlog.lib.model.OutputWrite
import dev.vulnlog.lib.render.Message
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
        val write = writeOrFail(writeOutputIfChanged(target.toPath(), source.readText(), createDirectories = true))
        val verb = if (write == OutputWrite.Unchanged) StatusVerb.UNCHANGED else StatusVerb.WROTE
        logger.log(Message.Status(formatStatus(verb, target.absolutePath)))
    }
}
