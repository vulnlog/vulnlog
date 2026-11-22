// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app.report

import dev.vulnlog.lib.model.Project

data class ChangelogProjectsDiffer(
    val projects: List<Project>,
) : ChangelogOutcome.Failed
