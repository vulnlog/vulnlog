// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.reporting

import dev.vulnlog.lib.model.Project

data class ReportingChangelogProject(
    val project: Project,
    val releases: List<ReportingChangelogRelease>,
)
