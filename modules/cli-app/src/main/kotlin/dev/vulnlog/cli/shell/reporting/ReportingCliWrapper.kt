// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell.reporting

import com.github.ajalt.clikt.core.CliktCommand
import dev.vulnlog.cli.shell.exitCode
import dev.vulnlog.cli.shell.failWith
import dev.vulnlog.lib.app.ProjectsDiffer
import dev.vulnlog.lib.core.reporting.sharedProject
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.render.renderProjectsDiffer

fun CliktCommand.sharedProjectOrFail(files: List<VulnlogFile>): Project =
    sharedProject(files) ?: ProjectsDiffer(files.map { it.project }.distinct()).let { differ ->
        failWith(listOf(renderProjectsDiffer(differ)), exitCode(differ))
    }
