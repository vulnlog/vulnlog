// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.model.Project

data class ProjectsDiffer(
    val projects: List<Project>,
) : ImpactReportOutcome.Failed,
    ChangelogOutcome.Failed
