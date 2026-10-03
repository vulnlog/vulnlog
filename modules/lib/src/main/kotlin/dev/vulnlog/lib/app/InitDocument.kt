// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.core.init
import dev.vulnlog.lib.document.yaml.YamlWriter
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.VulnlogFile

/** The driver looks at the target, so the run itself touches no file. */
data class InitRequest(
    val organization: String,
    val name: String,
    val author: String,
    /** False for standard output, which has nothing to replace. */
    val targetExists: Boolean,
    val force: Boolean,
)

sealed interface InitOutcome {
    sealed interface Failed : InitOutcome

    /** Replacing a file takes `force`, so a mistyped path cannot wipe an existing Vulnlog file. */
    data object AlreadyExists : Failed

    data class Created(
        val vulnlogFile: VulnlogFile,
        val content: String,
    ) : InitOutcome
}

fun initDocument(request: InitRequest): InitOutcome {
    if (request.targetExists && !request.force) return InitOutcome.AlreadyExists
    val vulnlogFile = init(SchemaVersion.V1, request.organization, request.name, request.author)
    return InitOutcome.Created(vulnlogFile, YamlWriter.write(vulnlogFile))
}
