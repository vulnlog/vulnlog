// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.reporting

import dev.vulnlog.gradle.internal.failure
import dev.vulnlog.lib.app.ProjectsDiffer
import dev.vulnlog.lib.core.reporting.sharedProject
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.VulnlogFile

fun sharedProjectOrFail(files: List<VulnlogFile>): Project =
    sharedProject(files) ?: throw failure(ProjectsDiffer(files.map { it.project }.distinct()))
