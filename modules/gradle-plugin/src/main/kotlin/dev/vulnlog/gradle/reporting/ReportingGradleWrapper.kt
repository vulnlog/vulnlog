// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.reporting

import dev.vulnlog.lib.core.reporting.validateSharedProject
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.VulnlogFile
import org.gradle.api.GradleException

/** The project [files] share. Fails the build when their coordinates differ. */
fun sharedProjectOrFail(files: List<VulnlogFile>): Project =
    validateSharedProject(files)
        ?: throw GradleException("All input files must share the same project metadata.")
